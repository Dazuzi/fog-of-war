package com.fogofwar.fade;
import net.runelite.api.coords.WorldPoint;
final class FadingPlayerPredictor {
	WorldPoint getVelocity(WorldPoint lastLocation, WorldPoint twoTicksAgoLocation) {
		return twoTicksAgoLocation != null
				? new WorldPoint(lastLocation.getX() - twoTicksAgoLocation.getX(), lastLocation.getY() - twoTicksAgoLocation.getY(), 0)
				: new WorldPoint(0, 0, 0);
	}
	boolean isNearRenderLimit(WorldPoint lastLocation, WorldPoint localPlayerLocation, WorldPoint velocity, int renderDistance) {
		return isAtRenderLimit(lastLocation, localPlayerLocation, renderDistance) || isRunningNearLimit(lastLocation, localPlayerLocation, velocity, renderDistance);
	}
	WorldPoint getInitialFadeLocation(WorldPoint lastLocation, WorldPoint localPlayerLocation, WorldPoint velocity, boolean extrapolate, int renderDistance, boolean nearRenderLimit) {
		if (!extrapolate) return lastLocation;
		int dx = lastLocation.getX() - localPlayerLocation.getX();
		int dy = lastLocation.getY() - localPlayerLocation.getY();
		boolean onXEdge = Math.abs(dx) >= renderDistance - 1;
		boolean onYEdge = Math.abs(dy) >= renderDistance - 1;
		WorldPoint predictedNextLocation = new WorldPoint(lastLocation.getX() + velocity.getX(), lastLocation.getY() + velocity.getY(), lastLocation.getPlane());
		boolean isMovingIntoRenderArea = (onXEdge && dx * velocity.getX() < 0) || (onYEdge && dy * velocity.getY() < 0);
		if (isMovingIntoRenderArea) {
			int pushX = onXEdge ? Integer.signum(dx) : 0;
			int pushY = onYEdge ? Integer.signum(dy) : 0;
			return new WorldPoint(predictedNextLocation.getX() + pushX, predictedNextLocation.getY() + pushY, predictedNextLocation.getPlane());
		}
		WorldPoint initialFadeLocation = predictedNextLocation;
		if (nearRenderLimit) {
			int pushX = 0, pushY = 0;
			if (Math.abs(dx) > Math.abs(dy)) pushX = Integer.signum(dx);
			else if (Math.abs(dy) > Math.abs(dx)) pushY = Integer.signum(dy);
			else { pushX = Integer.signum(dx); pushY = Integer.signum(dy); }
			if ((pushX != 0 || pushY != 0) && predictedNextLocation.getPlane() == localPlayerLocation.getPlane()) {
				int x = predictedNextLocation.getX();
				int y = predictedNextLocation.getY();
				int lx = localPlayerLocation.getX();
				int ly = localPlayerLocation.getY();
				boolean moved = false;
				while (Math.max(Math.abs(x - lx), Math.abs(y - ly)) <= renderDistance) {
					x += pushX;
					y += pushY;
					moved = true;
				}
				if (moved) initialFadeLocation = new WorldPoint(x, y, predictedNextLocation.getPlane());
			}
		}
		return initialFadeLocation;
	}
	private boolean isAtRenderLimit(WorldPoint lastLocation, WorldPoint localPlayerLocation, int renderDistance) {
		return lastLocation.distanceTo(localPlayerLocation) >= renderDistance - 1;
	}
	private boolean isRunningNearLimit(WorldPoint lastLocation, WorldPoint localPlayerLocation, WorldPoint velocity, int renderDistance) {
		int velocityMagnitude = Math.abs(velocity.getX()) + Math.abs(velocity.getY());
		return lastLocation.distanceTo(localPlayerLocation) >= renderDistance - 2 && velocityMagnitude >= 2;
	}
}
