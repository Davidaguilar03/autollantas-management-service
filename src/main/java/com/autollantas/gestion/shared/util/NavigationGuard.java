package com.autollantas.gestion.shared.util;

/**
 * Interface for controllers that need to intercept navigation away from their view.
 *
 * loadView() in MainLayoutController will call canLeave() before replacing the view.
 * If canLeave() returns false, navigation is cancelled. The controller is responsible
 * for showing any confirmation dialog and resuming navigation when the user decides.
 */
public interface NavigationGuard {

    /**
     * Called before the current view is replaced by another.
     *
     * @param onProceed runnable to call if the controller decides navigation should continue
     * @return true to allow immediate navigation, false to block it (controller must call
     *         onProceed later if the user confirms)
     */
    boolean canLeave(Runnable onProceed);
}
