package liquibasewizard.wizards.bean;

import liquibasewizard.wizards.JDBCConnection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ChangeSet {

	private final Element changeSet;
	private final Document doc;
	private String statement;
	private boolean calculateCatalog;
	private int sequencial;

	public ChangeSet(Document doc, String statement, boolean calculateCatalog, int sequencial) {
		this.doc = doc;
		this.statement = statement;
		this.changeSet = doc.createElement("changeSet");
		this.calculateCatalog = calculateCatalog;
		this.sequencial = sequencial;
		this.processStatement();
	}

	/**
	 * Seta o ID da tag Changeset
	 * 
	 * @param id
	 */
	public final void setId(String id) {
		changeSet.setAttribute("id", id);
	}
	
	public final String getId() {
		return changeSet.getAttribute("id");
	}
	

	/**
	 * Seta o Author da tag changeset
	 * 
	 * @param author
	 */
	public final void setAuthor(String author) {
		changeSet.setAttribute("author", author);
	}

	public final void setFailOnError(boolean failOnError) {
		changeSet.setAttribute("failOnError", String.valueOf(failOnError));
	}

	/**
	 * Transforma o statement em uma tag changeset padrão do liquibase
	 */
	public final void processStatement() {
		if (statement.toUpperCase().contains("VIEW")) {
			this.processComment();
			Element createView = doc.createElement("createView");
			String viewName = statement.trim().substring(22, statement.toUpperCase().indexOf(" AS")).trim();
			createView.setAttribute("viewName", viewName);
			createView.setAttribute("replaceIfExists", "true");
			createView.setAttribute("catalogName", "0");
			if (calculateCatalog) {
				createView.setAttribute("catalogName", new JDBCConnection().getCatalogVersion(viewName));
			}
			createView.appendChild(doc.createTextNode("\n\t\t\t" + statement.substring(statement.toUpperCase().indexOf("SELECT")) + "\n\t\t"));
			getElement().appendChild(createView);
		} else if (statement.toUpperCase().contains("<PROCEDURE>")) {
			statement = statement.replaceAll("<(?i)procedure>", "").replaceAll("</(?i)procedure>", "");
			this.processComment();
			Element sql = doc.createElement("createProcedure");
			sql.appendChild(doc.createTextNode("\n\t\t\t" + statement + "\n\t\t"));
			getElement().appendChild(sql);
		} else {
			this.processComment();
			Element sql = doc.createElement("sql");
			sql.appendChild(doc.createTextNode("\n\t\t\t" + statement + "\n\t\t"));
			getElement().appendChild(sql);
		}
		
		if(this.getId().isEmpty()) {
			this.processId();
		}
	}

	/**
	 * Adiciona o Element comment ao changeset
	 */
	private final void processComment() {
		Element commentElement = doc.createElement("comment");
		String commentValue;
		statement = statement.trim();
		if (statement.startsWith("/*")) {
			commentValue = statement.substring(statement.indexOf("/*") + 2, statement.indexOf("*/"));
			statement = statement.replace(statement.substring(statement.indexOf("/*"), statement.indexOf("*/") + 2), "").trim();
		} else if (statement.startsWith("--")) {
			commentValue = statement.substring(statement.indexOf("--") + 2, statement.indexOf("\n"));
			statement = statement.replace(statement.substring(statement.indexOf("--"), statement.indexOf("\n") + 2), "").trim();
		} else {
			this.processId();
			commentValue = this.getId();
		}
		commentElement.appendChild(doc.createTextNode(commentValue.trim()));
		getElement().appendChild(commentElement);
	}

	private void processId() {
		StringBuilder id = new StringBuilder();
		id.append(this.sequencial).append("_");
		String sql = statement.trim().replaceAll("\\r|\r", "\n").replaceAll("\n", "").toUpperCase();

		try {

			if (containsAll(sql, "INSERT", "INTO")
					&& containsAny(sql, "CM_PARAM", "AUTSC2_PARAM")) {
				// Adicionando Parâmetros
				sql = sql.substring(sql.indexOf("('") + 2, sql.length());
				id.append("ADD_PARAM_").append(sql.substring(0, sql.indexOf("'")));
				
			} else if (containsAll(sql, "INSERT", "INTO")
					&& containsAny(sql, "CM_TERMOS", "AUTSC2_TERMOS")) {
				// Adicionando Parâmetros
				sql = statement.substring(statement.indexOf("('") + 2, statement.length());
				id.append("ADD_TERMO_").append(sql.substring(0, sql.indexOf("'")));

			} else if (containsAll(sql, "ADD", "CONSTRAINT", "PRIMARY", "KEY")) {
				// Adicionando PK
				sql = sql.trim().substring(sql.toUpperCase().indexOf("TABLE") + 5, sql.toUpperCase().indexOf("PRIMARY")).trim().replaceAll("\\s+", "_");
				sql = sql.toUpperCase().replace("CONSTRAINT_", "PK_");
				id.append(sql);

			} else if (containsAll(sql, "ADD", "CONSTRAINT", "FOREIGN", "KEY")) {
				// Adicionando FK
				sql = sql.trim().substring(sql.toUpperCase().indexOf("TABLE") + 5, sql.toUpperCase().indexOf("FOREIGN")).trim().replaceAll("\\s+", "_");
				sql = sql.toUpperCase().replace("CONSTRAINT_", "FK_");
				id.append(sql);

			} else if (containsAll(sql, "ALTER", "TABLE", "ADD")) {
				// Adicionando colunas
				sql = sql.trim().substring(0, sql.indexOf("("));
				id.append(sql.trim().substring(0, sql.lastIndexOf(" ")).replaceAll("\\s+", "_"));

			} else if (containsAll(sql, "COMMENT", "ON")
					&& containsAny(sql, "COLUMN", "TABLE")
					&& !containsAny(sql, "PRIMARY", "FOREIGN")) {
				// Adicionando comentários às colunas
				sql = sql.trim().substring(0, sql.toUpperCase().indexOf("IS ")).trim().replaceAll("\\s+", "_");
				id.append(sql);

			} else if (containsAll(sql, "CREATE", "INDEX", "ON")) {
				// Adicionando Index
				sql = sql.trim().substring(0, sql.indexOf("(")).trim().replaceAll("\\s+", "_");
				id.append(sql);
				setFailOnError(false);
			} else if (containsAll(sql, "CREATE", "TABLE")) {
				// Criando tabela
				sql = sql.trim().substring(0, sql.indexOf("(")).trim().replaceAll("\\s+", "_");
				id.append(sql);

			} else if (containsAll(sql, "CREATE", "SEQUENCE")) {
				// Criando Sequence
				sql = sql.trim().substring(0, sql.toUpperCase().trim().lastIndexOf(" ")).trim().replaceAll("\\s+", "_");
				id.append(sql);

			} else if (containsAll(sql, "CREATE", "REPLACE", "VIEW")) {
				// Atualizando views
				String viewName = statement.trim().substring(22, statement.toUpperCase().indexOf(" AS")).trim();
				id.append("UPDATE_VIEW_").append(viewName.toUpperCase());

			} else if (containsAll(sql, "CREATE", "REPLACE", "TRIGGER")) {
				// Atualizando trigger
				String triggerName = statement.trim().substring(statement.toUpperCase().indexOf("TRIGGER") + 8, statement.toUpperCase().indexOf("\n")).replaceAll("\"", "").trim();
				id.append("CREATE_TRIGGER_").append(triggerName.toUpperCase());

			}
		} catch (Exception e) {
			System.out.println(e);
		}

		// limitando a 254 caracteres para não estourar o tamanho da coluna na tabela(255)
		this.setId(id.substring(0, id.length() > 254 ? 254 : id.length()));
	}

	private boolean containsAll(String toProcess, String... ocurrences) {
		for (String ocurrence : ocurrences) {
			if (!toProcess.toUpperCase().contains(ocurrence.toUpperCase())) {
				return false;
			}
		}
		return true;
	}

	private boolean containsAny(String toProcess, String... ocurrences) {
		for (String ocurrence : ocurrences) {
			if (toProcess.toUpperCase().contains(ocurrence.toUpperCase())) {
				return true;
			}
		}
		return false;
	}

	public final Element getElement() {
		return changeSet;
	}

	public Document getDoc() {
		return doc;
	}

	public String getStatement() {
		return statement;
	}

	public boolean isCalculateCatalog() {
		return calculateCatalog;
	}

	public void setCalculateCatalog(boolean calculateCatalog) {
		this.calculateCatalog = calculateCatalog;
	}

	public int getSequencial() {
		return sequencial;
	}

	public void setSequencial(int sequencial) {
		this.sequencial = sequencial;
	}

	public Element getChangeSet() {
		return changeSet;
	}

	public void setStatement(String statement) {
		this.statement = statement;
	}

}
