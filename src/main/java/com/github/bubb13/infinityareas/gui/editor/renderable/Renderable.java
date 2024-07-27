
package com.github.bubb13.infinityareas.gui.editor.renderable;

import com.github.bubb13.infinityareas.misc.DoubleCorners;
import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

public interface Renderable
{
    DoubleCorners getCorners();
    boolean isEnabled();
    int sortWeight();
    void onRender(GraphicsContext canvasContext);
    boolean contains(Point2D point);
    boolean offerPressCapture(MouseEvent event);
    void onClicked(MouseEvent event);
    boolean offerDragCapture(MouseEvent event);
    void onDragged(MouseEvent event);
    void onBeforeSelected();
    void onAdditionalObjectSelected(Renderable renderable);
    void onReceiveKeyPress(KeyEvent event);
    void onUnselected();
    void delete();
    boolean listensToZoomFactorChanges();
    void onZoomFactorChanged(double zoomFactor);
}
