/*
 * Copyright 2016 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.basheditor;

import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.FixMethodOrder;

/**
 * Special project builder to support validation for all bash files in workspace
 * 
 * @author Albert Tregnaghi
 *
 */
public class BashValidationProjectBuilder extends IncrementalProjectBuilder {

	public static final String BUILDER_ID = "de.jcup.basheditor.validationbuilder";
	
	
	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
		IProject project = getProject();
		if (!project.isOpen()) {
			return null;
		}
		/* FIXME ATR, 19.9. 2017*/
		System.out.println("FIXME: build called");
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {

			IResourceDelta delta = getDelta(project);
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}

	protected void clean(IProgressMonitor monitor) throws CoreException {
		BashEditorUtil.cleanAllValidationErrors(monitor);
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		try {
			getProject().accept(new BashFullBuildVisitor(monitor));
		} catch (CoreException e) {
		}
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		delta.accept(new BashDeltaBuildVisitor(monitor));
	}

	class BashDeltaBuildVisitor implements IResourceDeltaVisitor {

		private IProgressMonitor monitor;
		private BashValidationHelper validationHelper;

		public BashDeltaBuildVisitor(IProgressMonitor monitor) {
			this.monitor=monitor;
			validationHelper = new BashValidationHelper(BashEditorUtil.getPreferences().getPreferenceStore());
		}

		@Override
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			if (isContainer(resource)) {
				return true;
			}
			if (!isBashFile(resource)) {
				return false;
			}
			int kind = delta.getKind();
			switch (kind) {
			case IResourceDelta.ADDED:
				executeValidation(validationHelper, monitor, resource);
				break;
			case IResourceDelta.REMOVED:
				BashEditorUtil.cleanValidationErrors(resource ,monitor);
				break;
			case IResourceDelta.CHANGED:
				executeValidation(validationHelper, monitor, resource);
				break;
			}
			// a bash file has no children so no scan necessary..
			return false;
		}
	}

	class BashFullBuildVisitor implements IResourceVisitor {
		private IProgressMonitor monitor;
		private BashValidationHelper validationHelper;

		public BashFullBuildVisitor(IProgressMonitor monitor) {
			this.monitor=monitor;
			validationHelper = new BashValidationHelper(BashEditorUtil.getPreferences().getPreferenceStore());
		}
		public boolean visit(IResource resource) throws CoreException {
			if (isContainer(resource)) {
				return true;
			}
			if (!isBashFile(resource)) {
				return false;
			}
			executeValidation(validationHelper,monitor, resource);
			// a bash file has no children so no scan necessary..
			return false;

		}
	}

	private boolean isBashFile(IResource resource) {
		if (resource == null) {
			return false;
		}
		if (resource.isPhantom()) {
			return false;
		}
		String name = resource.getName();
		if (name == null) {
			return false;
		}
		return name.endsWith(".s");
	}

	private void executeValidation(BashValidationHelper helper, IProgressMonitor monitor, IResource resource) {
		if (resource==null){
			return;
		}
		monitor.subTask("Bash validation started for "+resource.getName());
		helper.validateResource(resource, false);
		monitor.worked(1);
		
	}

	private boolean isContainer(IResource resource) {
		return resource instanceof IContainer;
	}
}
