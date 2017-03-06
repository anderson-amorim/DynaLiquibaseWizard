package liquibasewizard.wizards;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;

/**
 * The "New" wizard page allows setting the container for the new file as well as the file name. The page will only accept file name without the extension OR with the extension that matches the
 * expected one (xml).
 */

public class SampleNewWizardPage extends WizardPage {
	private Text containerText;

	private Text sqlFileText;

	private Text fileText;

	private Text authorText;

	private Text tetraText;

	private Text intraText;

	private Text fileNameText;

	private Spinner fileSequencial;

	private Button recalculateSequencial;

	private Button addToChangeLog;

	private Button calculateCatalog;

	private Button validateSql;

	private Combo systems;

	private ISelection selection;

	private String selectedPath;

	/**
	 * Constructor for SampleNewWizardPage.
	 * 
	 * @param pageName
	 */
	public SampleNewWizardPage(ISelection selection) {
		super("wizardPage");
		setTitle("Dynamix Liquibase Plugin");
		setDescription("Converte um arquivo SQL em um xml do Liquibase.");
		this.selection = selection;
		if (selection != null && selection.isEmpty() == false && selection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) selection;
			if (ssel.size() > 1)
				return;
			Object obj = ssel.getFirstElement();
			if (obj instanceof IResource) {
				IContainer container;
				if (obj instanceof IContainer) {
					container = (IContainer) obj;
				} else {
					container = ((IResource) obj).getParent();
				}
				selectedPath = container.getFullPath().toString();
			}
		}
	}

	/**
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		Label label = new Label(container, SWT.NULL);
		label.setText("&Container:");

		// Container
		containerText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		containerText.setText(selectedPath);
		containerText.setLayoutData(gd);
		containerText.setMessage("Selecione a pasta onde o Liquibase será criado");
		containerText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button button = new Button(container, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleContainerBrowse();
			}
		});

		// Sql file
		label = new Label(container, SWT.NULL);
		label.setText("&Sql File:");
		sqlFileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		sqlFileText.setLayoutData(gd);
		sqlFileText.setMessage("Selecione um arquivo SQL para ser convertido em Liquibase");
		sqlFileText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
			}
		});

		Button fileButton = new Button(container, SWT.PUSH);
		fileButton.setText("Browse...");
		fileButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleFileBrowse();
			}
		});
		
		
		filenameFields(container);

		// Output Resumed file
		label = new Label(container, SWT.PUSH);
		label.setText("&File name:");

		fileNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		fileNameText.setMessage("Nome resumido do Arquivo. Ex.: ResumoChamadoAlteracaoTetra");
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileNameText.setLayoutData(gd);

		label = new Label(container, SWT.PUSH);

		// Output Final file
		label = new Label(container, SWT.PUSH);
		label.setText("&Preview:");

		fileText = new Text(container, SWT.BORDER | SWT.SINGLE);
		fileText.setEnabled(false);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		fileText.setLayoutData(gd);
		label = new Label(container, SWT.PUSH);

		// Author Name
		label = new Label(container, SWT.PUSH);
		label.setText("&Author:");

		authorText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		authorText.setLayoutData(gd);
		authorText.setText(System.getProperty("user.name"));

		label = new Label(container, SWT.PUSH);

		// Add to changelog
		addToChangeLog = new Button(container, SWT.CHECK);
		addToChangeLog.setText("Adicionar arquivo ao db-changelog.xml");
		addToChangeLog.setSelection(true);
		setSpan(addToChangeLog, 3);

		// Calculate catalog version
		calculateCatalog = new Button(container, SWT.CHECK);
		calculateCatalog.setText("Calcular a versão do catalogName");
		calculateCatalog.setSelection(true);
		setSpan(calculateCatalog, 3);

		// Validate Sql
		validateSql = new Button(container, SWT.CHECK);
		validateSql.setText("Verificar se existem caracteres especiais no SQL");
		validateSql.setSelection(true);
		setSpan(validateSql, 3);

		// Dicas
		label = new Label(container, SWT.PUSH);
		label.setText("Observações:\n\n"
				+ "* Cada instrução do arquivo SQL deve ser separada por ';'.\n"
				+ "* O comentario ('--' ou '/* */') no início de cada instrução será usado na tag <comment>.\n"
				+ "* 'Calcular a versão do catalogName' pode deixar a geração do arquivo lenta dependendo da conexão.\n"
				+ "* Intruções PLSQL devem estar entre as tags <procedure></procedure>(incluindo os comentários)\n e serão adicionadas sempre ao final do xml.\n"
				+ "* Para garantir que a validação de caracteres especiais funcione corretamente, salve o\n arquivo SQL pelo PL-SQL Developer");
		label.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
		label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_END));
		setSpan(label, 3);

		initialize();
		dialogChanged();
		setControl(container);
		fileSequencial.setSelection(Integer.valueOf(getContainerFileCount()));
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @param container
	 * @version
	 * @tetra
	 * @intra
	 */
	private void filenameFields(Composite container) {
		
		boolean isCentralLiquibase = getContainerText().getText().contains("DEV-");

		ModifyListener listener = isCentralLiquibase ? getDevModifyListener() : getNormalModifyListener();

		// Tetra
		Label label;
		label = new Label(container, SWT.NULL);
		label.setText("&Jira:");
		
		Composite tetraIntra = getTetraIntraGroup(container, isCentralLiquibase ? 9 : 10);
		addTetraField(container, listener, tetraIntra);

		// Intra
		addIntraField(listener, tetraIntra);
		

		// Sequencial
		addSequencialField(listener, tetraIntra);

		// Recalcular
		addRecalculateField(tetraIntra);
		
		if (isCentralLiquibase) {
			// Sistemas
			addSystemField(listener, tetraIntra);
		}
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @return
	 * @version 
	 * @tetra 
	 * @intra 
	 */
	private ModifyListener getDevModifyListener() {
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				setDevFinalFileName();
			}
		};
		return listener;
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @return
	 * @version 
	 * @tetra 
	 * @intra 
	 */
	private ModifyListener getNormalModifyListener() {
		ModifyListener listener = new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				dialogChanged();
				setFinalFileName();
			}
		};
		return listener;
	}
	
	/**
	 * @author Anderson de Amorim
	 *
	 * @param tetraIntra
	 * @version
	 * @tetra
	 * @intra
	 */
	private void addRecalculateField(Composite tetraIntra) {
		recalculateSequencial = new Button(tetraIntra, SWT.BUTTON1);
		recalculateSequencial.setText("Recalcular");
		recalculateSequencial.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fileSequencial.setSelection(Integer.valueOf(getContainerFileCount()));
			}
		});
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @param listener
	 * @param tetraIntra
	 * @version
	 * @tetra
	 * @intra
	 */
	private void addSequencialField(ModifyListener listener, Composite tetraIntra) {
		Label label;
		label = new Label(tetraIntra, SWT.NULL);
		label = new Label(tetraIntra, SWT.NULL);
		label.setText("&Sequencial:");
		fileSequencial = new Spinner(tetraIntra, SWT.BORDER | SWT.SINGLE);
		fileSequencial.setMinimum(0);
		fileSequencial.setMaximum(999);
		fileSequencial.setIncrement(1);
		fileSequencial.setTextLimit(3);
		fileSequencial.addModifyListener(listener);
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @param listener
	 * @param tetraIntra
	 * @version
	 * @tetra
	 * @intra
	 */
	private void addIntraField(ModifyListener listener, Composite tetraIntra) {
		Label label;
		label = new Label(tetraIntra, SWT.NULL);
		label = new Label(tetraIntra, SWT.NULL);
		label.setText("&Intra:");
		intraText = new Text(tetraIntra, SWT.BORDER | SWT.SINGLE);
		intraText.addModifyListener(listener);
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @param listener
	 * @param tetraIntra
	 * @version
	 * @tetra
	 * @intra
	 */
	private void addTetraField(Composite container, ModifyListener listener, Composite tetraIntra) {
		tetraText = new Text(tetraIntra, SWT.BORDER | SWT.SINGLE);
		tetraText.addModifyListener(listener);
	}

	/**
	 * @author Anderson de Amorim
	 *
	 * @param listener
	 * @param tetraIntra
	 * @version
	 * @tetra
	 * @intra
	 */
	private void addSystemField(ModifyListener listener, Composite tetraIntra) {
		systems = new Combo(tetraIntra, SWT.BORDER | SWT.SINGLE | SWT.READ_ONLY | SWT.SIMPLE | SWT.DROP_DOWN);
		systems.setItems("AUTSC2", "CMAGNET");
		systems.select(0);
		systems.addModifyListener(listener);
	}

	private Composite getTetraIntraGroup(Composite container, int columns) {
		Composite tetraIntra = new Composite(container, SWT.SINGLE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = columns;
		gridLayout.marginLeft = -11;
		tetraIntra.setLayout(gridLayout);
		GridData gridData = new GridData(GridData.FILL, GridData.CENTER, true, false);
		gridData.horizontalSpan = 2;
		tetraIntra.setLayoutData(gridData);
		return tetraIntra;
	}

	private void setSpan(Control label, Integer span) {
		GridData gridData = new GridData(GridData.VERTICAL_ALIGN_END);
		gridData.horizontalSpan = span;
		gridData.horizontalAlignment = GridData.FILL;
		label.setLayoutData(gridData);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use.
	 */

	private void initialize() {
		if (getContainerText().getText().contains("DEV-")) {
			setDevFinalFileName();
			fileNameText.addModifyListener(getDevModifyListener());
		} else {
			setFinalFileName();
			fileNameText.addModifyListener(getNormalModifyListener());
		}
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for the container field.
	 */

	private void handleContainerBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin.getWorkspace().getRoot(), false, "Select new file container");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				containerText.setText(((Path) result[0]).toString());
			}
		}
	}

	/**
	 * Uses the standard file selection dialog to choose the new value for the sql file field.
	 */

	private void handleFileBrowse() {
		FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
		fd.setText("Select a sql file");
		String[] filterExt = { "*.sql" };
		fd.setFilterExtensions(filterExt);
		String selected = fd.open();
		if (!"".equals(selected)) {
			sqlFileText.setText(selected);
		}
	}

	/**
	 * Ensures that both text fields are set.
	 */

	private void dialogChanged() {
		IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(new Path(getContainerName()));
		String fileName = getFileName();

		if (getContainerName().length() == 0) {
			updateStatus("File container must be specified");
			return;
		}
		if (container == null || (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("File container must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}

		if (getSqlFileName().length() == 0) {
			updateStatus("Sql file must be specified");
			return;
		}

		if (!getIntraText().getText().isEmpty()
				&& !getIntraText().getText().matches("\\d+")) {
			updateStatus("Intra must be a numeric value");
			return;
		}

		if (getFileSequencial().getText().length() == 0) {
			updateStatus("Sequencial must be specified");
			return;
		}

		if (!getFileSequencial().getText().isEmpty()
				&& !getFileSequencial().getText().matches("\\d+")) {
			updateStatus("Sequencial must be a numeric value");
			return;
		}

		if (getFileNameText().getText().length() == 0) {
			updateStatus("File Name must be specified");
			return;
		}

		int dotLoc = fileName.lastIndexOf('.');
		if (dotLoc != -1) {
			String ext = fileName.substring(dotLoc + 1);
			if (ext.equalsIgnoreCase("xml") == false) {
				updateStatus("File extension must be \"xml\"");
				return;
			}
		}
		updateStatus(null);
	}

	private void setFinalFileName() {
		StringBuilder text = new StringBuilder();
		text.append(getContainerName().substring(getContainerName().lastIndexOf("/") + 1)).append("-");

		if (isNotEmpty(getFileSequencial())) {
			text.append(String.format("%03d", Integer.valueOf(getFileSequencial().getText())));
		}
		if (isNotEmpty(getTetraText())) {
			text.append("-").append(getTetraText().getText());
		}
		if (isNotEmpty(getIntraText())) {
			text.append("-").append("INTRA").append(getIntraText().getText());
		}
		if (isNotEmpty(getFileNameText())) {
			text.append("-").append(getFileNameText().getText().replace(" ", "_"));
		}
		text.append(".xml");
		getFileText().setText(text.toString());
	}

	private void setDevFinalFileName() {
		StringBuilder text = new StringBuilder();

		if (isNotEmpty(getFileSequencial())) {
			text.append(String.format("%03d", Integer.valueOf(getFileSequencial().getText())));
		}
		if (isNotEmpty(getSystems())) {
			text.append("-").append(getSystems().getText());
		}
		if (isNotEmpty(getContainerText())) {
			// Adiciona a versao
			text.append("-").append(getContainerText().getText().substring(getContainerText().getText().lastIndexOf("DEV-") + 4));
		}
		if (isNotEmpty(getTetraText())) {
			text.append("-").append("").append(getTetraText().getText());
		}
		if (isNotEmpty(getIntraText())) {
			text.append("-").append("INTRA").append(getIntraText().getText());
		}
		if (isNotEmpty(getFileNameText())) {
			text.append("-").append(getFileNameText().getText().replace(" ", "_"));
		}

		text.append(".xml");
		getFileText().setText(text.toString());
	}

	private boolean isNotEmpty(Text value) {
		return value != null && value.getText() != null && !value.getText().isEmpty();
	}

	private boolean isNotEmpty(Spinner value) {
		return value != null && value.getText() != null && !value.getText().isEmpty();
	}

	private boolean isNotEmpty(Combo value) {
		return value != null && value.getText() != null && !value.getText().isEmpty();
	}

	private String getContainerFileCount() {
		try {
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IResource resource = root.findMember(new Path(getContainerName()));
			IContainer container = (IContainer) resource;
			Integer count = 0;
			for (IResource member : container.members()) {
				if (member.getName().endsWith(".xml") && !member.getName().contains("db-changelog")) {
					count++;
				}
			}
			return String.valueOf(count + 1);
		} catch (Exception e) {
			return null;
		}
	}

	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	public String getSqlFileName() {
		return sqlFileText.getText();
	}

	public String getContainerName() {
		return containerText.getText();
	}

	public String getFileName() {
		return fileText.getText();
	}

	public String getAuthorName() {
		return authorText.getText();
	}

	public boolean isAddToChangelogChecked() {
		return this.addToChangeLog.getSelection();
	}

	public boolean isCalculateCatalogChecked() {
		return this.calculateCatalog.getSelection();
	}

	public boolean isValidateSqlChecked() {
		return this.validateSql.getSelection();
	}

	public Text getContainerText() {
		return containerText;
	}

	public void setContainerText(Text containerText) {
		this.containerText = containerText;
	}

	public Text getSqlFileText() {
		return sqlFileText;
	}

	public void setSqlFileText(Text sqlFileText) {
		this.sqlFileText = sqlFileText;
	}

	public Text getFileText() {
		return fileText;
	}

	public void setFileText(Text fileText) {
		this.fileText = fileText;
	}

	public Text getAuthorText() {
		return authorText;
	}

	public void setAuthorText(Text authorText) {
		this.authorText = authorText;
	}

	public Text getTetraText() {
		return tetraText;
	}

	public void setTetraText(Text tetraText) {
		this.tetraText = tetraText;
	}

	public Text getIntraText() {
		return intraText;
	}

	public void setIntraText(Text intraText) {
		this.intraText = intraText;
	}

	public Text getFileNameText() {
		return fileNameText;
	}

	public void setFileNameText(Text fileNameText) {
		this.fileNameText = fileNameText;
	}

	public Button getAddToChangeLog() {
		return addToChangeLog;
	}

	public void setAddToChangeLog(Button addToChangeLog) {
		this.addToChangeLog = addToChangeLog;
	}

	public Button getCalculateCatalog() {
		return calculateCatalog;
	}

	public void setCalculateCatalog(Button calculateCatalog) {
		this.calculateCatalog = calculateCatalog;
	}

	public ISelection getSelection() {
		return selection;
	}

	public void setSelection(ISelection selection) {
		this.selection = selection;
	}

	public Button getValidateSql() {
		return validateSql;
	}

	public void setValidateSql(Button validateSql) {
		this.validateSql = validateSql;
	}

	public Spinner getFileSequencial() {
		return fileSequencial;
	}

	public void setFileSequencial(Spinner fileSequencial) {
		this.fileSequencial = fileSequencial;
	}

	public Button getRecalculateSequencial() {
		return recalculateSequencial;
	}

	public void setRecalculateSequencial(Button recalculateSequencial) {
		this.recalculateSequencial = recalculateSequencial;
	}

	public final Combo getSystems() {
		return systems;
	}

	public final void setSystems(Combo systems) {
		this.systems = systems;
	}

}