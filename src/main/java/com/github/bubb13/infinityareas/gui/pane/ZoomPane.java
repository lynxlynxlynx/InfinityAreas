
package com.github.bubb13.infinityareas.gui.pane;

import com.github.bubb13.infinityareas.gui.region.PartiallyRenderedImage;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;

import java.awt.image.BufferedImage;

public class ZoomPane extends NotifyingScrollPane
{
    ////////////////////
    // Private Fields //
    ////////////////////

    private final PartiallyRenderedImage partialImage = new PartiallyRenderedImage();
    private double zoomFactor = 1;

    /////////////////////////
    // Public Constructors //
    /////////////////////////

    public ZoomPane()
    {
        super();
        init();
    }

    ////////////////////
    // Public Methods //
    ////////////////////

    public void setImage(final BufferedImage image, boolean resetZoomFactor)
    {
        partialImage.setImage(image);
        if (resetZoomFactor)
        {
            zoomFactor = 1;
            partialImage.setZoomFactor(zoomFactor);
        }
        setHvalue(0);
        setVvalue(0);
    }

    public void setImage(final BufferedImage image)
    {
        setImage(image, true);
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void init()
    {
        final Pane pane = new Pane();
        pane.getChildren().add(partialImage);

        setContent(pane);
        setPannable(true);
        setFocusTraversable(false);
        setStyle("-fx-focus-color: transparent; -fx-faint-focus-color: transparent;");

        addEventFilter(ScrollEvent.SCROLL, this::onScroll);
        partialImage.setOnMouseClicked(this::onMouseClick);
    }

    private void onMouseClick(final MouseEvent event)
    {
        final double imageViewX = event.getX();
        final double imageViewY = event.getY();

        final int imageX = (int)(imageViewX / zoomFactor);
        final int imageY = (int)(imageViewY / zoomFactor);

        System.out.printf("Click at (%d,%d)\n", imageX, imageY);
    }

    private void onScroll(final ScrollEvent event)
    {
        if (event.isControlDown())
        {
            event.consume();
            onZoom(event.getDeltaY());
        }
    }

    private void onZoom(final double deltaY)
    {
        final Bounds viewportBounds = getViewportBounds();
        final double viewportWidth = viewportBounds.getWidth();
        final double viewportHeight = viewportBounds.getHeight();
        final double viewportEffectiveWidth = viewportWidth / zoomFactor;
        final double viewportEffectiveHeight = viewportHeight / zoomFactor;

        final Bounds imageViewBounds = partialImage.getBoundsInLocal();
        final double imageViewWidth = imageViewBounds.getWidth();
        final double imageViewHeight = imageViewBounds.getHeight();

        final double hMax = getHmax();
        final double vMax = getVmax();

        final double hRel = getHvalue() / hMax;
        final double vRel = getVvalue() / vMax;

        final double xLeft = hRel * (imageViewWidth - viewportWidth) / zoomFactor;
        final double yTop = vRel * (imageViewHeight - viewportHeight) / zoomFactor;

        if (deltaY > 0)
        {
            zoomFactor *= 1.1;
        }
        else if (deltaY < 0)
        {
            zoomFactor *= 0.9;
        }

        final BufferedImage image = partialImage.getImage();
        final double newImageViewWidth = image.getWidth() * zoomFactor;
        final double newImageViewHeight = image.getHeight() * zoomFactor;
        final double newViewportEffectiveWidth = viewportWidth / zoomFactor;
        final double newViewportEffectiveHeight = viewportHeight / zoomFactor;

        partialImage.setZoomFactor(zoomFactor);
        layout();

        final double targetCenterX = xLeft + (viewportEffectiveWidth / 2);
        final double targetCenterY = yTop + (viewportEffectiveHeight / 2);

        final double targetXLeft = targetCenterX - (newViewportEffectiveWidth / 2);
        final double targetYLeft = targetCenterY - (newViewportEffectiveHeight / 2);

        final double newHRel = targetXLeft * zoomFactor / (newImageViewWidth - viewportWidth);
        final double newVRel = targetYLeft * zoomFactor / (newImageViewHeight - viewportHeight);

        final double newHVal = newHRel * hMax;
        final double newVVal = newVRel * vMax;

        setHvalue(newHVal);
        setVvalue(newVVal);
    }
}
