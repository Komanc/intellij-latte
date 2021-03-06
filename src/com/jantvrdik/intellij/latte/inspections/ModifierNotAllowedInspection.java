package com.jantvrdik.intellij.latte.inspections;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.jantvrdik.intellij.latte.config.LatteConfiguration;
import com.jantvrdik.intellij.latte.config.LatteMacro;
import com.jantvrdik.intellij.latte.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ModifierNotAllowedInspection extends LocalInspectionTool {


	@NotNull
	@Override
	public String getShortName() {
		return "ModifierNotAllowed";
	}

	@Nullable
	@Override
	public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull final InspectionManager manager, final boolean isOnTheFly) {
		if (!(file instanceof LatteFile)) {
			return null;
		}

		final List<ProblemDescriptor> problems = new ArrayList<ProblemDescriptor>();
		file.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
			@Override
			public void visitElement(PsiElement element) {
				if (element instanceof LatteMacroTag) {
					checkClassicMacro((LatteMacroTag) element, problems, manager, isOnTheFly);

				} else {
					super.visitElement(element);
				}
			}
		});
		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}

	private static void checkClassicMacro(
			LatteMacroTag macroTag,
			@NotNull List<ProblemDescriptor> problems,
			@NotNull final InspectionManager manager,
			final boolean isOnTheFly
	) {
		String name = macroTag.getMacroName();
		LatteMacro macro = LatteConfiguration.INSTANCE.getMacro(macroTag.getProject(), name);
		if (macro == null || macro.allowedModifiers) {
			return;
		}

		LatteMacroContent content = macroTag.getMacroContent();
		if (content == null) {
			return;
		}

		content.acceptChildren(new PsiRecursiveElementWalkingVisitor() {
			@Override
			public void visitElement(PsiElement element) {
				if (element instanceof LatteMacroModifier) {
					String description = "Modifiers are not allowed here";
					ProblemDescriptor problem = manager.createProblemDescriptor(element, description, true, ProblemHighlightType.GENERIC_ERROR, isOnTheFly);
					problems.add(problem);

				} else {
					super.visitElement(element);
				}
			}
		});
	}
}
