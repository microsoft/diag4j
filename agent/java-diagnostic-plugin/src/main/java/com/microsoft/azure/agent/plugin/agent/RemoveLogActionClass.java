package com.microsoft.azure.agent.plugin.agent;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.microsoft.azure.agent.plugin.agent.service.KubernetesService;

public class RemoveLogActionClass extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getDataContext().getData(CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            return;
        }
        if (KubernetesService.defaultPodName == null || KubernetesService.defaultContainerName == null) {
            // Popup window to prompt the user to select a default pod first
            com.intellij.openapi.ui.Messages.showWarningDialog(
                    project,
                    "No active pod and container selected.\nPlease select an active pod before proceeding.",
                    "Select Active Pod"
            );
            return; // Exit the action if no default pod is set
        }

        try {
            KubernetesService.removeLog(KubernetesService.defaultPodName, KubernetesService.defaultContainerName);
            com.intellij.openapi.ui.Messages.showInfoMessage("Successfully remove all around logs", "Successfully Remove Log");
        } catch (Exception e) {
            com.intellij.openapi.ui.Messages.showErrorDialog(
                    project,
                    "Failed to remove all around logs: " + e.getMessage(),
                    "Request Error"
            );
        }
    }

    public static boolean isPsiFieldOrMethodOrClass(PsiElement psiElement) {
        return psiElement instanceof PsiField || psiElement instanceof PsiClass || psiElement instanceof PsiMethod || psiElement instanceof PsiJavaFile;
    }

}
