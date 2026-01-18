package de.cavdar.gui.itsq.dialog;

import de.cavdar.itsq.migration.model.MigrationProblem;
import de.cavdar.itsq.migration.model.MigrationProblem.ProblemType;
import de.cavdar.itsq.migration.model.MigrationProblem.Resolution;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dialog zur interaktiven Behandlung von Migrationsproblemen.
 * Ermoeglicht dem Benutzer zu waehlen, wie ein bestimmtes Problem
 * behandelt werden soll und optional die Entscheidung fuer aehnliche Probleme zu merken.
 */
public class MigrationProblemDialog extends JDialog {

    private final MigrationProblem problem;
    private final Map<ProblemType, Resolution> rememberedDecisions;
    private Resolution selectedResolution;

    private JLabel labelProblemType;
    private JLabel labelLocation;
    private JLabel labelCrefo;
    private JTextArea textAreaDetails;
    private JCheckBox checkBoxRemember;
    private JButton buttonSkip;
    private JButton buttonCopyAnyway;
    private JButton buttonAbort;

    public MigrationProblemDialog(Window owner, MigrationProblem problem,
                                  Map<ProblemType, Resolution> rememberedDecisions) {
        super(owner, "Problem bei der Migration", ModalityType.APPLICATION_MODAL);
        this.problem = problem;
        this.rememberedDecisions = rememberedDecisions != null
                ? rememberedDecisions
                : new EnumMap<>(ProblemType.class);
        this.selectedResolution = null;

        initComponents();
        setupListeners();
        populateData();

        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(new EmptyBorder(15, 15, 15, 15));

        // Icon-Panel
        JPanel iconPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane.warningIcon"));
        iconPanel.add(iconLabel);
        add(iconPanel, BorderLayout.WEST);

        // Inhalts-Panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        // Problemtyp
        JPanel typePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        typePanel.add(new JLabel("Typ: "));
        labelProblemType = new JLabel();
        labelProblemType.setFont(labelProblemType.getFont().deriveFont(Font.BOLD));
        typePanel.add(labelProblemType);
        contentPanel.add(typePanel);

        // Position
        JPanel locationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        locationPanel.add(new JLabel("Pfad: "));
        labelLocation = new JLabel();
        locationPanel.add(labelLocation);
        contentPanel.add(locationPanel);

        // Crefo
        JPanel crefoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        crefoPanel.add(new JLabel("Crefo: "));
        labelCrefo = new JLabel();
        crefoPanel.add(labelCrefo);
        contentPanel.add(crefoPanel);

        // Details
        contentPanel.add(Box.createVerticalStrut(10));
        contentPanel.add(new JLabel("Details:"));
        textAreaDetails = new JTextArea(4, 40);
        textAreaDetails.setEditable(false);
        textAreaDetails.setLineWrap(true);
        textAreaDetails.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textAreaDetails);
        scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(scrollPane);

        // Merken-Checkbox
        contentPanel.add(Box.createVerticalStrut(10));
        checkBoxRemember = new JCheckBox("Diese Entscheidung fuer alle aehnlichen Probleme merken");
        checkBoxRemember.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(checkBoxRemember);

        add(contentPanel, BorderLayout.CENTER);

        // Button-Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        buttonSkip = new JButton("Ueberspringen");
        buttonCopyAnyway = new JButton("Trotzdem kopieren");
        buttonAbort = new JButton("Abbrechen");

        buttonPanel.add(buttonSkip);
        buttonPanel.add(buttonCopyAnyway);
        buttonPanel.add(buttonAbort);

        add(buttonPanel, BorderLayout.SOUTH);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    }

    private void setupListeners() {
        buttonSkip.addActionListener(e -> selectResolution(Resolution.SKIP));
        buttonCopyAnyway.addActionListener(e -> selectResolution(Resolution.COPY_ANYWAY));
        buttonAbort.addActionListener(e -> selectResolution(Resolution.ABORT));
    }

    private void populateData() {
        labelProblemType.setText(problem.getType().getDisplayName());
        labelLocation.setText(problem.getLocationString());
        labelCrefo.setText(problem.getCrefoNr() != null ? problem.getCrefoNr().toString() : "-");

        StringBuilder details = new StringBuilder();
        details.append(problem.getDetails());
        if (problem.getFilePath() != null && !problem.getFilePath().isEmpty()) {
            details.append("\n\nDatei: ").append(problem.getFilePath());
        }
        textAreaDetails.setText(details.toString());

        // Deaktiviere "Trotzdem kopieren" fuer bestimmte Problemtypen
        if (problem.getType() == ProblemType.CUSTOMER_NO_VALID_TESTCASES) {
            buttonCopyAnyway.setEnabled(false);
        }
    }

    private void selectResolution(Resolution resolution) {
        this.selectedResolution = resolution;
        problem.setResolution(resolution);
        problem.setRememberDecision(checkBoxRemember.isSelected());

        if (checkBoxRemember.isSelected()) {
            rememberedDecisions.put(problem.getType(), resolution);
        }

        dispose();
    }

    /**
     * Zeigt den Dialog und gibt die gewaehlte Loesung zurueck.
     * Gibt null zurueck wenn der Dialog ohne Auswahl geschlossen wurde.
     */
    public Resolution showDialog() {
        setVisible(true);
        return selectedResolution;
    }

    /**
     * Gibt die gewaehlte Loesung zurueck.
     */
    public Resolution getSelectedResolution() {
        return selectedResolution;
    }

    /**
     * Prueft ob der Benutzer die Entscheidung merken moechte.
     */
    public boolean isRememberDecision() {
        return checkBoxRemember.isSelected();
    }

    /**
     * Gibt die Map der gemerkten Entscheidungen zurueck (von diesem Dialog modifiziert).
     */
    public Map<ProblemType, Resolution> getRememberedDecisions() {
        return rememberedDecisions;
    }

    /**
     * Zeigt einen Dialog fuer ein Migrationsproblem und gibt die Loesung zurueck.
     * Falls eine gemerkte Entscheidung fuer diesen Problemtyp existiert, wird sie sofort zurueckgegeben.
     */
    public static Resolution showProblemDialog(Window owner, MigrationProblem problem,
                                               Map<ProblemType, Resolution> rememberedDecisions) {
        // Pruefe auf gemerkte Entscheidung
        if (rememberedDecisions != null) {
            Resolution remembered = rememberedDecisions.get(problem.getType());
            if (remembered != null) {
                problem.setResolution(remembered);
                return remembered;
            }
        }

        // Zeige Dialog
        MigrationProblemDialog dialog = new MigrationProblemDialog(owner, problem, rememberedDecisions);
        return dialog.showDialog();
    }
}
