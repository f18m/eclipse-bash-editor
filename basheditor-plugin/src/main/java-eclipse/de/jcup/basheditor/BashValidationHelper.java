package de.jcup.basheditor;

import static de.jcup.basheditor.preferences.BashEditorValidationPreferenceConstants.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

import de.jcup.basheditor.script.BashError;
import de.jcup.basheditor.script.BashScriptModel;
import de.jcup.basheditor.script.BashScriptModelBuilder;

public class BashValidationHelper {
	
	private BashScriptModelBuilder modelBuilder;

	public BashValidationHelper(IPreferenceStore store){
		modelBuilder = new BashScriptModelBuilder();

		final boolean validateBlocks = store.getBoolean(VALIDATE_BLOCK_STATEMENTS.getId());
		final boolean validateDo = store.getBoolean(VALIDATE_DO_STATEMENTS.getId());
		final boolean validateIf = store.getBoolean(VALIDATE_IF_STATEMENTS.getId());
		final boolean validateFunctions = store.getBoolean(VALIDATE_IF_STATEMENTS.getId());

		modelBuilder.setIgnoreBlockValidation(!validateBlocks);
		modelBuilder.setIgnoreDoValidation(!validateDo);
		modelBuilder.setIgnoreIfValidation(!validateIf);
		modelBuilder.setIgnoreFunctionValidation(!validateFunctions);

	}
	public void validateResource(IResource resource, boolean async) {
		if (resource==null){
			return;
		}
		validateResources(Arrays.asList(resource), async);
	}
	
	public void validateResources(final List<IResource> resources, boolean async) {
		
		Runnable r = new Runnable() {

			@Override
			public void run() {
				for (IResource resource : resources) {
					if (resource == null) {
						continue;
					}
					validate(resource);
				}

			}

			private void validate(IResource resource) {
				if (resource==null){
					return;
				}
				IDocument document = resource.getAdapter(IDocument.class);
				String text = BashEditorUtil.getDocumentText(document);
				if (text==null) {
					return;
				}
				BashScriptModel model = modelBuilder.build(text);

				if (model.hasErrors()) {
					Collection<BashError> errors = model.getErrors();
					for (BashError error : errors) {
						int startPos = error.getStart();
						int line;
						try {
							line = document.getLineOfOffset(startPos);
						} catch (BadLocationException e) {
							EclipseUtil.logError("Cannot get line offset for " + startPos, e);
							line = 0;
						}
						BashEditorUtil.addScriptError(resource, line, error);
					}
				}
			}
		};
		if (async){
			EclipseUtil.safeAsyncExec(r);
		}else{
			r.run();
		}
	}
}
