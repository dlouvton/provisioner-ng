package com.dlouvton.badger.provisioner.status;

import com.dlouvton.badger.provisioner.VagrantException;

/***
 * A listener for provisioner phases. 
 * Every Phase Implementation should implement this interface.
 * These methods will be called on certain events, and update the environment with the provision status. 
 */

public interface PhaseListener {
   public void markStarted();
   public void markCompleted();
   public void markError(VagrantException t, StatusCode code) throws VagrantException;
   public Phase getPhase();
}
