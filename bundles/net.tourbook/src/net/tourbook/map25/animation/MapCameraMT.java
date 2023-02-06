package net.tourbook.map25.animation;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;

import org.oscim.core.MapPosition;
import org.oscim.core.Tile;
import org.oscim.map.Map;
import org.oscim.renderer.GLViewport;

/**
 * Using MT version to have access to {@link #mMapPosition}
 * <p>
 * Original source {@link org.oscim.gdx.poi3d.MapCamera}
 */
public class MapCameraMT extends Camera {

   private final Map mMap;

   MapPosition       mMapPosition = new MapPosition();

   public MapCameraMT(final Map map) {

      mMap = map;

      this.near = 1;
      this.far = 8;
   }

   public void setMapPosition(final double x, final double y, final double scale) {

      mMapPosition.x = x;
      mMapPosition.y = y;

      mMapPosition.setScale(scale);
   }

   public void setPosition(final MapPosition pos) {

      mMapPosition.copy(pos);

      this.viewportWidth = mMap.getWidth();
      this.viewportHeight = mMap.getHeight();
   }

   @Override
   public void update() {}

   @Override
   public void update(final boolean updateFrustum) {

   }

   public void update(final GLViewport viewport) {

      final double scale = (viewport.pos.scale * Tile.SIZE);

      final float x = (float) ((mMapPosition.x - viewport.pos.x) * scale);
      final float y = (float) ((mMapPosition.y - viewport.pos.y) * scale);
      final float z = (float) (viewport.pos.scale / mMapPosition.scale);

      viewport.proj.get(projection.getValues());
      viewport.mvp.setTransScale(x, y, z);
      viewport.mvp.setValue(10, z);
      viewport.mvp.multiplyLhs(viewport.view);
      viewport.mvp.get(view.getValues());

      combined.set(projection);

      Matrix4.mul(combined.val, view.val);

      //if (updateFrustum) {
      invProjectionView.set(combined);
      Matrix4.inv(invProjectionView.val);
      frustum.update(invProjectionView);
      //}
   }

}
