
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.GlobalState;
import com.github.bubb13.infinityareas.game.Game;
import com.github.bubb13.infinityareas.game.resource.Area;
import com.github.bubb13.infinityareas.game.resource.WED;
import com.github.bubb13.infinityareas.gui.control.UnderlinedButton;
import com.github.bubb13.infinityareas.gui.dialog.ErrorAlert;
import com.github.bubb13.infinityareas.gui.editor.Editor;
import com.github.bubb13.infinityareas.gui.editor.EditorCommons;
import com.github.bubb13.infinityareas.gui.editor.GenericPolygon;
import com.github.bubb13.infinityareas.gui.editor.editmode.DrawPolygonEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.QuickSelectEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.areapane.AreaPaneNormalEditMode;
import com.github.bubb13.infinityareas.gui.editor.editmode.areapane.TrapRegionOptionsPane;
import com.github.bubb13.infinityareas.gui.editor.renderable.Renderable;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableActor;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderableClippedLine;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePoint;
import com.github.bubb13.infinityareas.gui.editor.renderable.RenderablePolygon;
import com.github.bubb13.infinityareas.misc.DoubleCorners;
import com.github.bubb13.infinityareas.misc.IntPoint;
import com.github.bubb13.infinityareas.misc.LoadingStageTracker;
import com.github.bubb13.infinityareas.misc.ReadableDoublePoint;
import com.github.bubb13.infinityareas.misc.TaskTrackerI;
import com.github.bubb13.infinityareas.misc.TrackedTask;
import com.github.bubb13.infinityareas.util.ImageUtil;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

public class AreaPane extends StackPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    // GUI
    private final CheckBox renderRegionsCheckbox = new CheckBox("Render Regions");

    private final ZoomPane zoomPane = new ZoomPane();
    private final Editor editor = new Editor(zoomPane, this);
    {
        final Comparator<Renderable> renderingComparator = Comparator.comparingInt(Renderable::sortWeight);
        editor.setRenderingComparator(renderingComparator);
        editor.setInteractionComparator(renderingComparator.reversed());
    }

    private final StackPane rightPane = new StackPane();
    private Node curRightNode;

    private VBox defaultRightNode;
    private TrapRegionOptionsPane trapRegionOptionsPane = new TrapRegionOptionsPane(editor);

    private Area area;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public AreaPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public TrackedTask<Void> setSourceTask(final Game.ResourceSource source)
    {
        return new SetAreaTask(source);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void reset()
    {
        editor.enterEditMode(AreaPaneNormalEditMode.class);

        for (final Area.Actor actor : area.actors())
        {
            new AreaActor(actor);
        }

        for (final Area.Region region : area.regions())
        {
            if (region.getType() == 0 && region.getbTrapped() == 1)
            {
                new TrapRegion(region);
            }
        }
    }

    private void init()
    {
        ///////////////
        // Main HBox //
        ///////////////

            final HBox mainHBox = new HBox();

            ///////////////
            // Main VBox //
            ///////////////

            final VBox mainVBox = new VBox();
            mainVBox.setFocusTraversable(false);
            mainVBox.setPadding(new Insets(5, 0, 0, 10));

                //////////////////
                // Toolbar HBox //
                //////////////////

                final HBox toolbar = new HBox();
                toolbar.setPadding(new Insets(0, 0, 5, 0));

                final Button saveButton = new Button("Save");
                saveButton.setOnAction((ignored) -> this.onSave());

                final Region padding1 = new Region();
                padding1.setPadding(new Insets(0, 0, 0, 5));

                final Button drawPolygonButton = new UnderlinedButton("Draw Polygon");
                drawPolygonButton.setOnAction((ignored) -> editor.enterEditMode(DrawPolygonEditMode.class));

                final Region padding2 = new Region();
                padding2.setPadding(new Insets(0, 0, 0, 5));

                final Button bisectLine = new UnderlinedButton("Bisect Line");
                bisectLine.setOnAction((ignored) -> EditorCommons.onBisectLine(editor));

                final Region padding3 = new Region();
                padding3.setPadding(new Insets(0, 0, 0, 5));

                final Button quickSelect = new UnderlinedButton("Quick Select");
                quickSelect.setOnAction((ignored) -> editor.enterEditMode(QuickSelectEditMode.class));

                toolbar.getChildren().addAll(saveButton, padding1, drawPolygonButton,
                    padding2, bisectLine, padding3, quickSelect);

            VBox.setVgrow(zoomPane, Priority.ALWAYS);
            mainVBox.getChildren().addAll(toolbar, zoomPane);

            ////////////////////
            // Side Pane VBox //
            ////////////////////

            defaultRightNode = new VBox();
            defaultRightNode.setMinWidth(150);
            defaultRightNode.setPadding(new Insets(5, 10, 10, 10));

                //////////////////////////////
                // Render Polygons Checkbox //
                //////////////////////////////

                renderRegionsCheckbox.selectedProperty().addListener((observable, oldValue, newValue) ->
                    onRenderRegionsChanged(newValue));

            defaultRightNode.getChildren().addAll(renderRegionsCheckbox);

        changeRightNode(defaultRightNode);
        mainHBox.getChildren().addAll(mainVBox, rightPane);
        getChildren().add(mainHBox);

        editor.registerEditMode(AreaPaneNormalEditMode.class, () -> new AreaPaneNormalEditMode(editor));
    }

    private void onSave()
    {
        final Path overridePath = GlobalState.getGame().getRoot().resolve("override");
        try
        {
            Files.createDirectories(overridePath);
        }
        catch (final Exception e)
        {
            ErrorAlert.openAndWait("Failed to save area", e);
        }

        final FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Output File");
        fileChooser.setInitialDirectory(overridePath.toFile());
        fileChooser.setInitialFileName(area.getSource().getIdentifier().resref() + ".ARE");

        GlobalState.pushModalStage(null);
        final File selectedFile = fileChooser.showSaveDialog(null);
        GlobalState.popModalStage(null);

        if (selectedFile == null)
        {
            return;
        }

        area.saveTask(selectedFile.toPath())
            .trackWith(new LoadingStageTracker())
            .onFailed((e) -> ErrorAlert.openAndWait("Failed to save area", e))
            .start();
    }

    private void changeRightNode(final Node newNode)
    {
        if (newNode != curRightNode)
        {
            curRightNode = newNode;
            final ObservableList<Node> children = rightPane.getChildren();
            children.clear();
            children.add(newNode);
        }
    }

    private void onRenderRegionsChanged(final boolean newValue)
    {
//        if (!newValue)
//        {
//            for (final Renderable renderable : editor.selectedObjects())
//            {
//                if (renderable instanceof TrapRegion.TrapPolygon)
//                {
//                    editor.unselect(renderable);
//                }
//            }
//        }

        editor.requestDraw();
    }

    /////////////////////
    // Private Classes //
    /////////////////////

    private class AreaActor extends RenderableActor
    {
        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public AreaActor(final Area.Actor actor)
        {
            super(editor, actor);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        @Override
        public int sortWeight()
        {
            return 2;
        }
    }

    private class TrapRegion
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Area.Region region;
        private TrapPolygon trapPolygon;
        private boolean selected;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TrapRegion(final Area.Region region)
        {
            this.region = region;
            trapPolygon = new TrapPolygon(region.getPolygon());
            new TrapLaunchPosition(region.getTrapLaunchPoint());
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        public class TrapPolygon extends RenderablePolygon<GenericPolygon>
        {
            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public TrapPolygon(final GenericPolygon polygon)
            {
                super(editor, polygon);
                setRenderFill(true);
            }

            ////////////////////
            // Public Methods //
            ////////////////////

            @Override
            public boolean isEnabled()
            {
                return renderRegionsCheckbox.isSelected();
            }

            @Override
            public void onClicked(final MouseEvent mouseEvent)
            {
                if (mouseEvent.getButton() != MouseButton.PRIMARY)
                {
                    return;
                }

                editor.select(this);
            }

            @Override
            public void onBeforeSelected()
            {
                selected = true;
                editor.unselectAll();
                editor.requestDraw();

                trapRegionOptionsPane.setRegion(region);
                changeRightNode(trapRegionOptionsPane);
            }

            @Override
            public void onAdditionalObjectSelected(final Renderable renderable)
            {
                editor.unselect(this);
            }

            @Override
            public void onReceiveKeyPress(final KeyEvent event)
            {
                if (event.getCode() == KeyCode.ESCAPE)
                {
                    event.consume();
                    editor.unselectAll();
                }
            }

            @Override
            public void onUnselected()
            {
                changeRightNode(defaultRightNode);
                selected = false;
                editor.requestDraw();
            }

            @Override
            public void onRender(final GraphicsContext canvasContext)
            {
                super.onRender(canvasContext);

                if (selected)
                {
                    final DoubleCorners corners = getCorners();

                    final Point2D canvasPointTopLeft = editor.sourceToAbsoluteCanvasDoublePosition(
                        corners.topLeftX(), corners.topLeftY());

                    final Point2D canvasPointBottomRight = editor.sourceToAbsoluteCanvasDoublePosition(
                        corners.bottomRightExclusiveX(), corners.bottomRightExclusiveY());

                    canvasContext.setLineWidth(1);
                    canvasContext.setStroke(Color.rgb(0, 255, 0));
                    canvasContext.strokeRect(
                        canvasPointTopLeft.getX(), canvasPointTopLeft.getY(),
                        canvasPointBottomRight.getX() - canvasPointTopLeft.getX(),
                        canvasPointBottomRight.getY() - canvasPointTopLeft.getY()
                    );
                }
            }

            ///////////////////////
            // Protected Methods //
            ///////////////////////

            @Override
            protected Color getLineColor()
            {
                return Color.RED;
            }

            @Override
            protected void deleteBackingObject()
            {
                region.delete();
            }
        }

        /////////////////////
        // Private Classes //
        /////////////////////

        private class TrapLaunchPosition extends RenderablePoint<IntPoint>
        {
            ////////////////////
            // Private Fields //
            ////////////////////

            /////////////////////////
            // Public Constructors //
            /////////////////////////

            public TrapLaunchPosition(final IntPoint point)
            {
                super(AreaPane.this.editor, point);
                new TrapLaunchPositionLine();
            }

            ////////////////////
            // Public Methods //
            ////////////////////

            @Override
            public boolean isEnabled()
            {
                return renderRegionsCheckbox.isSelected() && TrapRegion.this.selected;
            }

            @Override
            public int sortWeight()
            {
                return 3;
            }

            @Override
            public boolean offerPressCapture(final MouseEvent event)
            {
                return event.getButton() == MouseButton.PRIMARY;
            }

            @Override
            public boolean offerDragCapture(final MouseEvent event)
            {
                return true;
            }

            @Override
            public void delete() {}

            /////////////////////
            // Private Classes //
            /////////////////////

            private class TrapLaunchPositionLine extends RenderableClippedLine<ReadableDoublePoint>
            {
                ////////////////////
                // Private Fields //
                ////////////////////

                private final Collection<Rectangle2D> exclusionRectangles = new ArrayList<>();

                /////////////////////////
                // Public Constructors //
                /////////////////////////

                public TrapLaunchPositionLine()
                {
                    super(AreaPane.this.editor);
                    setBackingPoints(new BackingObjectPointProxy(), new TrapPolygonCenterProxy());
                }

                ////////////////////
                // Public Methods //
                ////////////////////

                @Override
                public boolean isEnabled()
                {
                    return TrapLaunchPosition.this.isEnabled();
                }

                @Override
                public int sortWeight()
                {
                    return TrapLaunchPosition.this.sortWeight();
                }

                ///////////////////////
                // Protected Methods //
                ///////////////////////

                @Override
                protected Collection<Rectangle2D> getCanvasExclusionRects()
                {
                    exclusionRectangles.clear();
                    exclusionRectangles.add(editor.cornersToAbsoluteCanvasRectangle(TrapLaunchPosition.this.getCorners()));
                    exclusionRectangles.add(editor.cornersToAbsoluteCanvasRectangle(trapPolygon.getCorners()));
                    return exclusionRectangles;
                }

                @Override
                protected Color getLineColor()
                {
                    return Color.MAGENTA;
                }

                /////////////////////
                // Private Classes //
                /////////////////////

                private class BackingObjectPointProxy implements ReadableDoublePoint
                {
                    @Override
                    public double getX()
                    {
                        return backingObject.getX();
                    }

                    @Override
                    public double getY()
                    {
                        return backingObject.getY();
                    }
                }

                private class TrapPolygonCenterProxy implements ReadableDoublePoint
                {
                    @Override
                    public double getX()
                    {
                        final DoubleCorners corners = trapPolygon.getCorners();
                        return (corners.topLeftX() + corners.bottomRightExclusiveX()) / 2;
                    }

                    @Override
                    public double getY()
                    {
                        final DoubleCorners corners = trapPolygon.getCorners();
                        return (corners.topLeftY() + corners.bottomRightExclusiveY()) / 2;
                    }
                }
            }
        }
    }

    private class SetAreaTask extends TrackedTask<Void>
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Game.ResourceSource source;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public SetAreaTask(final Game.ResourceSource source)
        {
            this.source = source;
        }

        ///////////////////////
        // Protected Methods //
        ///////////////////////

        @Override
        protected Void doTask() throws Exception
        {
            final TaskTrackerI tracker = getTracker();
            tracker.updateMessage("Processing area ...");
            tracker.updateProgress(0, 1);

            final Area area = new Area(source);
            area.load(getTracker());
            AreaPane.this.area = area;

            final WED.Graphics wedGraphics = area.newGraphics().getWedGraphics();
            wedGraphics.renderOverlays(getTracker(), 0, 1, 2, 3, 4);
            final BufferedImage image = ImageUtil.copyArgb(wedGraphics.getImage());

            editor.reset(image.getWidth(), image.getHeight());
            reset();

            waitForFxThreadToExecute(() -> zoomPane.setImage(image));
            return null;
        }
    }
}
