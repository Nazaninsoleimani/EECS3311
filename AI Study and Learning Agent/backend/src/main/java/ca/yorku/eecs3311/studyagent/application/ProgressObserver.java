package ca.yorku.eecs3311.studyagent.application;

import ca.yorku.eecs3311.studyagent.domain.ProgressEvent;

/** Observer of the Observer pattern: reacts when a concept's mastery changes. */
public interface ProgressObserver {
    void onProgressChanged(ProgressEvent event);
}
