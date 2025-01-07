package com.microsoft.azure.agent.plugin.agent;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.agent.plugin.agent.service.KubernetesService;
import org.jetbrains.annotations.NotNull;

public class AddLogActionClass extends AnAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        DataContext dataContext = e.getDataContext();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
        if (isPsiFieldOrMethodOrClass(psiElement)) {
//            final boolean psiElementInEnum = OgnlPsUtils.psiElementInEnum(psiElement);
//            if(Boolean.FALSE.equals(getSupportEnum()) && psiElementInEnum){
//                e.getPresentation().setEnabled(false);
//                return;
//            }
            e.getPresentation().setEnabled(true);
            return;
        }
        e.getPresentation().setEnabled(false);
    }


    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        Editor editor = event.getDataContext().getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (project == null || editor == null) {
            return;
        }

        // Get the file and caret offset
        PsiFile psiFile = event.getDataContext().getData(com.intellij.openapi.actionSystem.CommonDataKeys.PSI_FILE);
        int offset = editor.getCaretModel().getOffset();

        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        // Find the PsiElement at the caret position
        PsiElement elementAtCaret = psiFile.findElementAt(offset);

        // Find the containing method
        PsiMethod psiMethod = PsiTreeUtil.getParentOfType(elementAtCaret, PsiMethod.class);
        if (psiMethod != null) {
            String methodName = psiMethod.getName();

            // Find the containing class
            PsiClass containingClass = psiMethod.getContainingClass();
            if (containingClass != null) {
                String className = containingClass.getQualifiedName();


                if (KubernetesService.defaultPodName == null || KubernetesService.defaultContainerName == null || KubernetesService.defaultNamespace == null) {
                    // Popup window to prompt the user to select a default pod first
                    com.intellij.openapi.ui.Messages.showWarningDialog(
                            project,
                            "No active pod and container selected.\nPlease select an active pod before proceeding.",
                            "Select Active Pod"
                    );
                    return; // Exit the action if no default pod is set
                }

                try {
                    KubernetesService.addLog(KubernetesService.defaultPodName, KubernetesService.defaultContainerName, className, methodName);
                    com.intellij.openapi.ui.Messages.showInfoMessage("Successfully add log around " + className + ":" + methodName, "Successfully Add Log");
                } catch (Exception e) {
                    com.intellij.openapi.ui.Messages.showErrorDialog(
                            project,
                            "Failed to add all around logs: " + e.getMessage(),
                            "Request Error"
                    );
                }
            }
        }
    }

    public static boolean isPsiFieldOrMethodOrClass(PsiElement psiElement) {
        return psiElement instanceof PsiField || psiElement instanceof PsiClass || psiElement instanceof PsiMethod || psiElement instanceof PsiJavaFile;
    }

}
