/*
 * Copyright 2019 Albert Tregnaghi
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
package de.jcup.basheditor.debug.launch;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.widgets.Shell;

import de.jcup.basheditor.BashEditorActivator;
import de.jcup.basheditor.InfoPopup;
import de.jcup.basheditor.debug.BashDebugConstants;
import de.jcup.basheditor.debug.element.BashDebugTarget;
import de.jcup.basheditor.debug.element.FallbackBashDebugTarget;
import de.jcup.basheditor.preferences.BashEditorPreferences;
import de.jcup.eclipse.commons.ui.EclipseUtil;

public class BashDebugLaunchConfigurationDelegate extends LaunchConfigurationDelegate {

	private BashDebugTarget target;
	private TerminalLauncher terminalLauncher = new TerminalLauncher();

	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		if (!mode.equals(ILaunchManager.DEBUG_MODE)) {
			throw new IllegalStateException("Ony debug mode supported, not :" + mode);
		}
		if (target != null) {
			try {
				target.disconnect();
			} catch (Exception e) {
				EclipseUtil.logError("Debug target disconnect failed!", e, BashEditorActivator.getDefault());
			}
		}
		String program = configuration.getAttribute(BashDebugConstants.LAUNCH_ATTR_BASH_PROGRAM, "");
		String params = configuration.getAttribute(BashDebugConstants.LAUNCH_ATTR_BASH_PARAMS, "");

		IWorkspaceRoot root = EclipseUtil.getWorkspace().getRoot();
		IFile programFileResource = (IFile) root.findMember(program);
		if (programFileResource==null) {
		    String message = "Was not able to find bash script '"+program+"' in workspace.";
		    Status status = new Status(Status.ERROR, BashEditorActivator.getDefault().getPluginID(), message);
		    EclipseUtil.safeAsyncExec(()->ErrorDialog.openError(EclipseUtil.getActiveWorkbenchShell(), "Launch failed", "Bash script to launch not found.", status));
		    
		    FallbackBashDebugTarget fallbackTarget = new FallbackBashDebugTarget(launch,"Bash script not found");
		    launch.addDebugTarget(fallbackTarget);
		    fallbackTarget.terminate();
		    return;
		}
		File programFile = programFileResource.getLocation().toFile();
		
		boolean stopOnStartup = configuration.getAttribute(BashDebugConstants.LAUNCH_ATTR_STOP_ON_STARTUP, false);
		launch.setAttribute(BashDebugConstants.LAUNCH_ATTR_STOP_ON_STARTUP, Boolean.toString(stopOnStartup));
		int port = configuration.getAttribute(BashDebugConstants.LAUNCH_ATTR_SOCKET_PORT, BashDebugConstants.DEFAULT_DEBUG_PORT);
		boolean canDoAutoRun = getPreferences().isAutomaticLaunchInExternalTerminalEnabled();
		
		IProcess process = new BashRemoteProcess(launch);
		target = new BashDebugTarget(launch, process, port,programFileResource);
		if (!target.startDebugSession()) {
		    target.disconnect();
		    FallbackBashDebugTarget fallbackTarget = new FallbackBashDebugTarget(launch, "Not able to start debug session");
            launch.addDebugTarget(fallbackTarget);
            fallbackTarget.terminate();
			return;
		}
		launch.addDebugTarget(target);
		/* debug process is started, so launch terminal or inform */
		if (canDoAutoRun) {
			terminalLauncher.execute(programFile, params, getPreferences().getXTerminalSnippet());

		} else {
			EclipseUtil.safeAsyncExec(new Runnable() {

				@Override
				public void run() {
					Shell shell = EclipseUtil.getSafeDisplay().getActiveShell();

					String titleText = "Bash launch necessary";
					String infoText = "You have only started the debug remote connection.\nThe bash program is currently not started.";
					String subMessage = "Either you start your bash program from commandline\nor you change your preferences to launch in terminal";

					InfoPopup popup = new InfoPopup(shell, titleText, infoText, null);
					popup.setSubMessage(subMessage);
					popup.setLinkToPreferencesId("basheditor.eclipse.gradleeditor.preferences.BashEditorDebugPreferencePage");
					popup.setLinkToPreferencesText("Change behaviour in <a href=\"https://github.com/de-jcup/eclipse-bash-editor\">preferences</a>");
					popup.open();
				}

			});
		}
	}

	private BashEditorPreferences getPreferences() {
		return BashEditorPreferences.getInstance();
	}

}
