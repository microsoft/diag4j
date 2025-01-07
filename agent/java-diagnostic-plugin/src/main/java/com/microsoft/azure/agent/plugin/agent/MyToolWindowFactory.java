package com.microsoft.azure.agent.plugin.agent;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.agent.plugin.agent.entity.PodInfo;
import com.microsoft.azure.agent.plugin.agent.service.KubernetesService;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CompletableFuture;


public class MyToolWindowFactory implements ToolWindowFactory {
    private JPanel panel;
    private JTable podTable;
    private DefaultTableModel tableModel;
    private JLabel defaultPodInfoLabel;
    private JComboBox<String> namespaceComboBox;
    private JBTextField portField;
    private JLabel statusMessage;
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        KubernetesService.initializeClient();
        // Create a main panel
        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        addWelcomeLabel();
        addForwardPort();
        addNamespaceComboBox();
        addPodsTable();
        addButton();

        // Add content to the tool window
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);

        project.getMessageBus().connect().subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager) {
                if (toolWindow.isVisible()) {
                    // When expanded, recheck the port status
                    String portText = portField.getText().trim();
                    String message = getForwardPortMessage(portText);
                    statusMessage.setText(message);
                }
            }
        });
    }

    private void addWelcomeLabel() {
        // Add a label

        JLabel label = new JLabel("Welcome to Java Diagnostic Plugin");
        label.setFont(new Font(label.getFont().getName(), Font.BOLD, 16));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);

        defaultPodInfoLabel = new JLabel("Active pod info: None selected");
        defaultPodInfoLabel.setFont(new Font(label.getFont().getName(), Font.BOLD, 14));
        defaultPodInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(defaultPodInfoLabel);

    }

    private void addForwardPort() {
        // Create the panel for the port input
        JPanel portPanel = new JPanel();
        portPanel.setLayout(new BoxLayout(portPanel, BoxLayout.X_AXIS));
        portPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel portLabel = new JLabel("Agent port-forward port:");
        portLabel.setFont(new Font(portLabel.getFont().getName(), Font.PLAIN, 14));

        portField = new JBTextField(UrlConfig.getAgentPort()); // Default value
        portField.setMaximumSize(new Dimension(100, 30));

        statusMessage = new JLabel();
        statusMessage.setForeground(JBColor.RED);
        statusMessage.setPreferredSize(new Dimension(100, 30));
        statusMessage.setText(getForwardPortMessage(portField.getText().trim()));

        portPanel.add(portLabel);
        portPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        portPanel.add(portField);
        portPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        portPanel.add(statusMessage);

        panel.add(portPanel);
        // Add a listener to monitor changes
        portField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updatePortValue();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updatePortValue();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updatePortValue();
            }

            private void updatePortValue() {
                String portText = portField.getText().trim();
                UrlConfig.setAgentPort(portText);
                try {
                    statusMessage.setText(getForwardPortMessage(portText));
                    UrlConfig.setAgentPort(portText);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(panel, "Please enter a valid port number.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private void addNamespaceComboBox() {
        JPanel namespacePanel = new JPanel();
        namespacePanel.setLayout(new BoxLayout(namespacePanel, BoxLayout.X_AXIS));
        namespacePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel namespaceLabel = new JLabel("Select Namespace:");
        namespaceLabel.setFont(new Font(namespaceLabel.getFont().getName(), Font.PLAIN, 14));

        namespaceComboBox = new ComboBox<>();
        namespaceComboBox.setMaximumSize(new Dimension(200, 30));
        namespaceComboBox.setPreferredSize(new Dimension(200, 30));

        namespacePanel.add(namespaceLabel);
        namespacePanel.add(Box.createRigidArea(new Dimension(10, 0)));
        namespacePanel.add(namespaceComboBox);

        panel.add(namespacePanel);


        List<String> namespaces = KubernetesService.listNamespaces();
        for (String namespace : namespaces) {
            namespaceComboBox.addItem(namespace);
        }
        try {
            if (isPortOpen("localhost", Integer.parseInt(portField.getText().trim()))) {
                KubernetesService.defaultNamespace = (String) namespaceComboBox.getSelectedItem();
                loadPods();
            }
        } catch (Exception e) {
            // ignore
            e.printStackTrace();
        }
        // Add action listener to namespace selection
        namespaceComboBox.addActionListener(e -> {
            String selectedNamespace = (String) namespaceComboBox.getSelectedItem();
            if (selectedNamespace != null && !selectedNamespace.isEmpty()) {
                KubernetesService.defaultNamespace = selectedNamespace;
                loadPods();
            }
        });
    }


    private void addPodsTable() {
        String[] columnNames = {"Name", "PodIp", "Phase", "IsAttach"};
        tableModel = new DefaultTableModel(columnNames, 0);
        podTable = new JBTable(tableModel);

        JScrollPane scrollPane = new JBScrollPane(podTable);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        scrollPane.setPreferredSize(new Dimension(panel.getWidth(), 200)); // Limit height

        panel.add(scrollPane);

        podTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = podTable.rowAtPoint(e.getPoint());
                int column = podTable.columnAtPoint(e.getPoint());
                // Check if it is a right-click
                if (SwingUtilities.isRightMouseButton(e)) {
                    // Get the pod name from the clicked row
                    String podName = (String) tableModel.getValueAt(row, 0);

                    // Create a popup menu
                    JPopupMenu popupMenu = new JPopupMenu();
                    JMenuItem setDefaultItem = new JMenuItem("Select as the active one");

                    // Add action listener for the menu item
                    setDefaultItem.addActionListener(event -> {
                        // Fetch container names for the selected pod
                        CompletableFuture.runAsync(() -> {
                            try {
                                List<String> containerNames = KubernetesService.getContainerNames(podName);

                                SwingUtilities.invokeLater(() -> {
                                    if (containerNames.isEmpty()) {
                                        JOptionPane.showMessageDialog(panel, "No containers found for pod: " + podName);
                                        return;
                                    }

                                    // Show a dialog with container selection
                                    String selectedContainer = (String) JOptionPane.showInputDialog(
                                            panel,
                                            "Select a container to set as active:",
                                            "Container Selection",
                                            JOptionPane.PLAIN_MESSAGE,
                                            null,
                                            containerNames.toArray(),
                                            containerNames.get(0)
                                    );

                                    // If a container is selected, set it as default
                                    if (selectedContainer != null) {
                                        String infoText = String.format("Active pod info: Pod - %s, Container - %s",
                                                podName, selectedContainer);
                                        setActivePodInfo(infoText, podName, selectedContainer);
                                    }
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(panel, "Failed to fetch container names: " + ex.getMessage());
                                });
                            }
                        });
                    });

                    // Add the menu item to the popup menu
                    popupMenu.add(setDefaultItem);

                    // Show the popup menu at the location of the mouse click
                    popupMenu.show(podTable, e.getX(), e.getY());

                } else {
                    // Check if a pod name (first column) was clicked
                    if (column == 0) {
                        String podName = (String) tableModel.getValueAt(row, 0);

                        // Fetch and show container names
                        CompletableFuture.runAsync(() -> {
                            try {
                                // Fetch container names for the selected pod
                                List<String> containerNames = KubernetesService.getContainerNames(podName);

                                // Show a dialog with a list of container names for the user to choose from
                                SwingUtilities.invokeLater(() -> {
                                    if (containerNames.isEmpty()) {
                                        JOptionPane.showMessageDialog(panel, "No containers found for pod: " + podName);
                                        return;
                                    }

                                    String selectedContainer = (String) JOptionPane.showInputDialog(
                                            panel,
                                            "Select a container to attach:",
                                            "Container Selection",
                                            JOptionPane.PLAIN_MESSAGE,
                                            null,
                                            containerNames.toArray(),
                                            containerNames.get(0)
                                    );

                                    // If a container is selected, proceed to show confirm dialog
                                    if (selectedContainer != null) {
                                        int option = JOptionPane.showConfirmDialog(panel,
                                                "Do you want to attach to container " + selectedContainer + " in pod " + podName + "?",
                                                "Confirm Attach",
                                                JOptionPane.YES_NO_OPTION);

                                        if (option == JOptionPane.YES_OPTION) {
                                            attachToContainer(podName, selectedContainer);
//                                        JOptionPane.showMessageDialog(panel, "Succeeded");
                                        }

                                    }
                                });
                            } catch (Exception ex) {
                                ex.printStackTrace();
                                SwingUtilities.invokeLater(() -> {
                                    JOptionPane.showMessageDialog(panel, "Failed to fetch container names: " + ex.getMessage());
                                });
                            }
                        });
                    }
                }
            }
        });
    }

    private void addButton() {
        JLabel infoLabel = new JLabel("Click the refresh button to refresh the namespace and pod list");
        infoLabel.setForeground(JBColor.BLUE);
        infoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(infoLabel);

        JButton button = new JButton("Refresh");
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Action listener to refresh namespaces and then reload pods based on the selected namespace
        button.addActionListener(e -> {
            KubernetesService.resetClient();
            refreshNamespaces(); // Step 1: Refresh namespaces
            String selectedNamespace = (String) namespaceComboBox.getSelectedItem();
            if (selectedNamespace != null && !selectedNamespace.isEmpty()) {
                KubernetesService.defaultNamespace = selectedNamespace;
                loadPods(); // Step 2: Load pods for the selected namespace
                setActivePodInfo("Active pod info: None selected", null, null);
            }
        });

        panel.add(button);
    }

    private void refreshNamespaces() {
        // Clear existing namespaces
        namespaceComboBox.removeAllItems();

        // Fetch updated list of namespaces
        List<String> namespaces = KubernetesService.listNamespaces();
        for (String namespace : namespaces) {
            namespaceComboBox.addItem(namespace);
        }

        // Optionally, set the default namespace
        if (namespaceComboBox.getItemCount() > 0) {
            namespaceComboBox.setSelectedIndex(0);
        }

        // clear the pod table too
        tableModel.setRowCount(0);
    }


    private void loadPods() {
        // Fetch the pod data asynchronously to avoid blocking the UI
        CompletableFuture.runAsync(() -> {
            try {
                // Call API to list all pods
                List<PodInfo> pods = KubernetesService.getAllPods();

                // Update the table model on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    tableModel.setRowCount(0); // Clear existing rows
                    for (PodInfo pod : pods) {
                        tableModel.addRow(new Object[]{pod.getName(), pod.getPodIp(), pod.getPhase(), pod.isAttach()});
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(panel, "Failed to load pods: " + e.getMessage());
            }
        });
    }

    private void attachToContainer(String podName, String containerName) {
        // Logic to send request to the backend
        try {
            // Call backend service to attach to the pod (e.g., via API call)
            KubernetesService.attachAgent(podName, containerName);
            JOptionPane.showMessageDialog(panel, "Successfully attached to pod: " + podName + " click refresh button to refresh pod list");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(panel, "Failed to attach agent to pod: " + e.getMessage());
        }
    }

    private void setActivePodInfo(String infoText, String podName, String selectedContainer) {
        defaultPodInfoLabel.setText(infoText);
        KubernetesService.defaultPodName = podName;
        KubernetesService.defaultContainerName = selectedContainer;
    }

    private static String getForwardPortMessage(String port) {
        return isPortOpen("localhost", Integer.valueOf(port)) ? "" : "Port closed! Check or re-enter.";
    }

    private static boolean isPortOpen(String host, int port) {
        try (Socket socket = new Socket(host, port)) {
            return true; // Port is open
        } catch (Exception e) {
            return false; // Port is closed
        }
    }

}
