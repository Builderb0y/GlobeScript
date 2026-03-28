package builderb0y.globescript;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.messages.MessageDialog;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

public class DisplayErrorAction extends AnAction {

	public Throwable throwable;

	public DisplayErrorAction(Throwable throwable) {
		super("Show Error");
		this.throwable = throwable;
	}

	@Override
	public void actionPerformed(@NotNull AnActionEvent event) {
		show(event.getProject(), this.throwable);
	}

	public static class DisplayErrorIntentionAction implements IntentionAction {

		public Throwable throwable;

		public DisplayErrorIntentionAction(Throwable throwable) {
			this.throwable = throwable;
		}

		@Override
		public @IntentionName @NotNull String getText() {
			return "Show error";
		}

		@Override
		public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
			return true;
		}

		@Override
		public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
			show(project, this.throwable);
		}

		@Override
		public boolean startInWriteAction() {
			return false;
		}

		@Override
		public @NotNull @IntentionFamilyName String getFamilyName() {
			return "GlobeScript";
		}

		@Override
		public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
			return IntentionPreviewInfo.EMPTY;
		}
	}

	public static void show(Project project, Throwable throwable) {
		StringWriter writer = new StringWriter(1024);
		try (PrintWriter printer = new PrintWriter(writer)) {
			throwable.printStackTrace(printer);
		}
		new MessageDialog(
			project,
			writer.toString(),
			"gs_env error:",
			new String[] { Messages.getOkButton() },
			0,
			Messages.getErrorIcon(),
			false
		) {

			{
				this.setSize(640, 640);
				this.setTitle("gs_env error");
			}
		}
		.show();
	}
}