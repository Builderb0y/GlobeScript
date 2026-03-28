package builderb0y.globescript;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

import builderb0y.globescript.DisplayErrorAction.DisplayErrorIntentionAction;

public class PsiErrorDisplay {

	public int startOffset, endOffset;
	public String text;
	public IntentionAction quickFix;

	public PsiErrorDisplay(int startOffset, int endOffset, String text) {
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.text = text;
	}

	public PsiErrorDisplay(int startOffset, int endOffset, Throwable throwable) {
		this(startOffset, endOffset, throwable.toString());
		this.quickFix = new DisplayErrorIntentionAction(throwable);
	}

	public void addTo(PsiElement element, AnnotationHolder holder) {
		int position = element != null ? element.getTextOffset() : 0;
		AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR, this.text).range(new TextRange(position + this.startOffset, position + this.endOffset));
		if (this.quickFix != null) builder = builder.withFix(this.quickFix);
		builder.create();
	}

	public PsiErrorDisplay withQuickFix(IntentionAction fix) {
		this.quickFix = fix;
		return this;
	}
}