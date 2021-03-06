package com.jantvrdik.intellij.latte.inspections;

import com.intellij.codeInsight.intention.IntentionManager;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementWalkingVisitor;
import com.jantvrdik.intellij.latte.config.LatteConfiguration;
import com.jantvrdik.intellij.latte.intentions.AddCustomLatteFunction;
import com.jantvrdik.intellij.latte.psi.LatteFile;
import com.jantvrdik.intellij.latte.psi.LattePhpMethod;
import com.jantvrdik.intellij.latte.settings.LatteCustomFunctionSettings;
import com.jantvrdik.intellij.latte.utils.LattePhpType;
import com.jantvrdik.intellij.latte.utils.LattePhpUtil;
import com.jetbrains.php.lang.psi.elements.Function;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodUsagesInspection extends BaseLocalInspectionTool {

	@NotNull
	@Override
	public String getShortName() {
		return "LatteMethodUsages";
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
				if (element instanceof LattePhpMethod) {
					if (((LattePhpMethod) element).isFunction()) {
						processFunction((LattePhpMethod) element, problems, manager, isOnTheFly);

					} else {
						processMethod((LattePhpMethod) element, problems, manager, isOnTheFly);
					}

				} else {
					super.visitElement(element);
				}
			}
		});

		return problems.toArray(new ProblemDescriptor[problems.size()]);
	}

	private void processFunction(
			LattePhpMethod element,
			@NotNull List<ProblemDescriptor> problems,
			@NotNull final InspectionManager manager,
			final boolean isOnTheFly
	) {
		String name = element.getMethodName();
		LatteCustomFunctionSettings customFunction = LatteConfiguration.INSTANCE.getFunction(element.getProject(), name);
		if (customFunction != null) {
			return;
		}

		Collection<Function> existing = LattePhpUtil.getFunctionByName(element.getProject(), name);
		if (existing.size() == 0) {
			LocalQuickFix addFunctionFix = IntentionManager.getInstance().convertToFix(new AddCustomLatteFunction(name));
			ProblemHighlightType type = ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
			String description = "Function '" + name + "' not found";
			ProblemDescriptor problem = manager.createProblemDescriptor(element, description, true, type, isOnTheFly, addFunctionFix);
			problems.add(problem);
		}
	}

	private void processMethod(
			LattePhpMethod element,
			@NotNull List<ProblemDescriptor> problems,
			@NotNull final InspectionManager manager,
			final boolean isOnTheFly
	) {
		LattePhpType phpType = element.getPhpType();

		boolean isFound = false;
		Collection<PhpClass> phpClasses = phpType.getPhpClasses(element.getProject());
		String methodName = element.getMethodName();
		if (phpClasses != null) {
			for (PhpClass phpClass : phpClasses) {
				for (Method method : phpClass.getMethods()) {
					if (method.getName().equals(methodName)) {
						if (method.getModifier().isPrivate()) {
							addProblem(manager, problems, element, "Used private method '" + methodName + "'", isOnTheFly);

						} else if (method.getModifier().isProtected()) {
							addProblem(manager, problems, element, "Used protected method '" + methodName + "'", isOnTheFly);
						}

						String description;
						boolean isStatic = ((LattePhpMethod) element).isStatic();
						if (isStatic && !method.getModifier().isStatic()) {
							description = "Method '" + methodName + "' is not static but called statically";
							addProblem(manager, problems, element, description, isOnTheFly);

						} else if (!isStatic && method.getModifier().isStatic()) {
							description = "Method '" + methodName + "' is static but called non statically";
							addProblem(manager, problems, element, description, isOnTheFly);
						}
						isFound = true;
					}
				}
			}
		}

		if (!isFound) {
			addProblem(manager, problems, element, "Method '" + methodName + "' not found", ProblemHighlightType.GENERIC_ERROR_OR_WARNING, isOnTheFly);
		}
	}
}
