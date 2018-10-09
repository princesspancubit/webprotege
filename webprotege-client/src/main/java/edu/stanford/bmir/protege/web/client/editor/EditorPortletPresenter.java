package edu.stanford.bmir.protege.web.client.editor;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import edu.stanford.bmir.protege.web.client.change.ChangeListViewPresenter;
import edu.stanford.bmir.protege.web.client.lang.DisplayNameRenderer;
import edu.stanford.bmir.protege.web.client.portlet.AbstractWebProtegePortletPresenter;
import edu.stanford.bmir.protege.web.client.portlet.PortletUi;
import edu.stanford.bmir.protege.web.client.tag.TagListPresenter;
import edu.stanford.bmir.protege.web.client.ui.ElementalUtil;
import edu.stanford.bmir.protege.web.shared.event.ClassFrameChangedEvent;
import edu.stanford.bmir.protege.web.shared.event.NamedIndividualFrameChangedEvent;
import edu.stanford.bmir.protege.web.shared.event.WebProtegeEventBus;
import edu.stanford.bmir.protege.web.shared.project.ProjectId;
import edu.stanford.bmir.protege.web.shared.selection.SelectionModel;
import edu.stanford.webprotege.shared.annotations.Portlet;
import org.semanticweb.owlapi.model.EntityType;
import org.semanticweb.owlapi.model.OWLEntity;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.semanticweb.owlapi.model.EntityType.*;

/**
 * Author: Matthew Horridge<br>
 * Stanford University<br>
 * Bio-Medical Informatics Research Group<br>
 * Date: 23/04/2013
 */
@Portlet(id = "portlets.EntityEditor",
        title = "Entity Editor",
        tooltip = "Displays a simple property-value oriented description of the selected class, property or individual for viewing and editing.")
public class EditorPortletPresenter extends AbstractWebProtegePortletPresenter {

    private final EditorPortletView view;

    private final TagListPresenter tagListPresenter;

    private final EditorPaneSimpleEditorPresenter editorPresenter;

    private final EditorPaneEntityChangesPresenter changesPresenter;

    private final Set<EntityType<?>> displayedTypes = new HashSet<>();

    @Inject
    public EditorPortletPresenter(
            @Nonnull ProjectId projectId,
            @Nonnull SelectionModel selectionModel,
            @Nonnull EditorPortletView view,
            @Nonnull TagListPresenter tagListPresenter,
            @Nonnull EditorPaneSimpleEditorPresenter editorPresenter,
            DisplayNameRenderer displayNameRenderer,
            @Nonnull EditorPaneEntityChangesPresenter changesPresenter) {
        super(selectionModel, projectId, displayNameRenderer);
        this.view = checkNotNull(view);
        this.tagListPresenter = checkNotNull(tagListPresenter);
        this.editorPresenter = checkNotNull(editorPresenter);
        this.changesPresenter = checkNotNull(changesPresenter);
        displayedTypes.addAll(Arrays.asList(
                CLASS,
                OBJECT_PROPERTY,
                DATA_PROPERTY,
                ANNOTATION_PROPERTY,
                NAMED_INDIVIDUAL,
                DATATYPE
        ));
    }

    public void setDisplayedTypes(EntityType<?> ... entityTypes) {
        displayedTypes.clear();
        displayedTypes.addAll(Arrays.asList(entityTypes));
    }

    @Override
    public void startPortlet(PortletUi portletUi, WebProtegeEventBus eventBus) {
        portletUi.setWidget(view);
        editorPresenter.setHasBusy(portletUi);
        editorPresenter.setEntityDisplay(this);
        AcceptsOneWidget editorContainer = view.addPane("Details");
        editorPresenter.start(editorContainer, eventBus);


        eventBus.addProjectEventHandler(getProjectId(),
                                        ClassFrameChangedEvent.CLASS_FRAME_CHANGED,
                                        this::handleClassFrameChangedEvent);
        eventBus.addProjectEventHandler(getProjectId(),
                                        NamedIndividualFrameChangedEvent.NAMED_INDIVIDUAL_CHANGED,
                                        this::handleIndividualFrameChangedEvent);
        tagListPresenter.start(view.getTagListViewContainer(), eventBus);

        AcceptsOneWidget changeListContainer = view.addPane("Changes");
        changesPresenter.start(changeListContainer, eventBus);
        handleAfterSetEntity(getSelectedEntity());
        setDisplaySelectedEntityNameAsSubtitle(true);
    }

    @Override
    protected void handleAfterSetEntity(Optional<OWLEntity> entity) {
        if(!entity.isPresent()  || !isDisplayedType(entity)) {
            setNothingSelectedVisible(true);
            setDisplayedEntity(Optional.empty());
            tagListPresenter.clear();
        }
        else {
            setNothingSelectedVisible(false);
            entity.ifPresent(editorPresenter::setEntity);
            tagListPresenter.setEntity(entity.get());
            changesPresenter.setEntity(entity.get());
        }
    }

    private void handleClassFrameChangedEvent(ClassFrameChangedEvent event) {
        if(displayedTypes.contains(CLASS) && getSelectedEntity().equals(Optional.of(event.getEntity()))) {
            reloadEditorIfNotActive();
        }
    }

    private void handleIndividualFrameChangedEvent(NamedIndividualFrameChangedEvent event) {
        if(displayedTypes.contains(NAMED_INDIVIDUAL) && getSelectedEntity().equals(Optional.of(event.getEntity()))) {
            reloadEditorIfNotActive();
        }
    }

    private void reloadEditorIfNotActive() {
        if(!isActive()) {
            handleAfterSetEntity(getSelectedEntity());
        }
    }


    private boolean isDisplayedType(Optional<OWLEntity> entity) {
        return entity.map(e -> displayedTypes.contains(e.getEntityType())).orElse(false);
    }

    @Override
    public void dispose() {
        editorPresenter.dispose();
        super.dispose();
    }

    private boolean isActive() {
        return ElementalUtil.isWidgetOrDescendantWidgetActive(view);
    }
}
