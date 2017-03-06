package liquibasewizard.wizards;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWizard;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

public class SampleNewWizard extends Wizard implements INewWizard {
	private SampleNewWizardPage page;
	private ISelection selection;

	/**
	 * Constructor for SampleNewWizard.
	 */
	public SampleNewWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the page to the wizard.
	 */

	public void addPages() {
		page = new SampleNewWizardPage(selection);
		addPage(page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in the wizard. We will create an operation and run it using wizard as execution context.
	 */
	public boolean performFinish() {
		try {
			final String containerName = page.getContainerName();
			final String fileName = page.getFileName();
			final String sqlFileName = page.getSqlFileName();
			final String authorName = page.getAuthorName();
			final boolean isAddToChangelogChecked = page.isAddToChangelogChecked();
			final boolean isCalculateCatalogChecked = page.isCalculateCatalogChecked();
			final boolean isValidateSqlChecked = page.isValidateSqlChecked();
			final IContainer container;
			try {
				container = getIContainer(containerName);
				if (getIFile(fileName, container).exists()
						&& !MessageDialog.openQuestion(getShell(), "Confirmação", "Já existe um arquivo com esse nome.\nDeseja sobrescrevê-lo?")) {
					return false;
				}
			} catch (CoreException e1) {
				throw new InvocationTargetException(e1);
			}
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException {
					try {
						doFinish(container, sqlFileName, fileName, authorName, monitor, isAddToChangelogChecked, isCalculateCatalogChecked, isValidateSqlChecked);
					} catch (CoreException e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			};
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * The worker method. It will find the container, create the file if missing or just replace its contents, and open the editor on the newly created file.
	 */
	private void doFinish(IContainer container, String sqlFileName, String fileName, String authorName, IProgressMonitor monitor, boolean isAddToChangelogChecked, boolean isCalculateCatalogChecked,
			boolean isValidateSqlChecked)
			throws CoreException {
		monitor.beginTask("Creating " + fileName, 2);
		final IFile file = getIFile(fileName, container);
		try {
			if (isValidateSqlChecked) {
				// Valida se existem caracteres inválidos no SQL
				isSqlFileValid(sqlFileName);
			}

			LiquibaseDocument liquibaseDoc = new LiquibaseDocument(authorName, sqlFileName, isCalculateCatalogChecked);
			InputStream stream = liquibaseDoc.transformToXml();
			if (file.exists()) {
				file.setContents(stream, true, true, monitor);
			} else {
				file.create(stream, true, monitor);
			}
			if (isAddToChangelogChecked) {
				liquibaseDoc.addToDbChangeLog(container.getLocation().toString(), fileName);
			}

			stream.close();
		} catch (Exception e) {
			throwCoreException(e.getMessage());
		}
		monitor.worked(1);
		monitor.setTaskName("Opening file for editing...");
		getShell().getDisplay().asyncExec(new Runnable() {
			public void run() {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				try {
					IDE.openEditor(page, file, true);
				} catch (PartInitException e) {
				}
			}
		});
		monitor.worked(1);
	}

	private IFile getIFile(String fileName, IContainer container) {
		final IFile file = container.getFile(new Path(fileName));
		return file;
	}

	private IContainer getIContainer(String containerName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource = root.findMember(new Path(containerName));
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		return container;
	}

	public void isSqlFileValid(String filePath) throws Exception {
		DataInputStream in = new DataInputStream(new FileInputStream(filePath));
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		String line;
		int lineCount = 1;
		StringBuilder errors = new StringBuilder();

		// Read File Line By Line
		while ((line = br.readLine()) != null) {
			// Remove todos os varchars do arquivo e compara se os caracteres do restante da intrução são ASCII printable characters
			// https://en.wikipedia.org/wiki/ASCII#ASCII_printable_characters
			if (!line.replaceAll("'.*?'", "").matches("^[\\x20-\\x7E]*$")) {
				errors.append("\nLinha: ").append(lineCount);
				errors.append("[ ").append(line).append(" ]");
			}
			lineCount++;
		}
		in.close();

		if (!errors.toString().isEmpty()) {
			throw new Exception("O arquivo SQL possui caracteres especiais!" + errors.toString());
		}
	}

	private void throwCoreException(String message) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, "LiquibaseWizard", IStatus.OK, message, null);
		throw new CoreException(status);
	}

	/**
	 * We will accept the selection in the workbench to see if we can initialize from it.
	 * 
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}