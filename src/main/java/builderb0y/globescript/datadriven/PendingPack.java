package builderb0y.globescript.datadriven;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public class PendingPack extends PendingElement {

	public static final FieldInjectorMap FIELDS = new FieldInjectorMap(
		new FieldInjector<PendingPack>("name", true) {

			@Override
			public void inject(PendingPack self, PendingDataContext context, @Nullable PsiElement value) {
				self.name = context.expectString(value);
			}
		},
		new FieldInjector<PendingPack>("data_folder", true) {

			@Override
			public void inject(PendingPack self, PendingDataContext context, @Nullable PsiElement value) {
				String path = context.expectString(value);
				if (path != null) self.dataFolder = context.projectData.contentRoot.findFileByRelativePath(path);
				if (self.dataFolder == null) context.addError(value, "Folder not found");
			}
		},
		new FieldInjector<PendingPack>("dependencies", false) {

			@Override
			public void inject(PendingPack self, PendingDataContext context, @Nullable PsiElement value) {
				self.dependencies = value == null ? new String[0] : context.expectArray(value, PendingDataContext::expectString).toArray(String[]::new);
			}
		}
	);

	@Override
	public FieldInjectorMap getFields() {
		return FIELDS;
	}

	public String name;
	public VirtualFile dataFolder;
	public String[] dependencies;

	public PendingPack(PendingDataContext context, PsiElement element) {
		super(context, element);
	}
}