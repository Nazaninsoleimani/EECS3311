# Week 5 - The Observer Pattern

## Motivation

Many programs contain an object whose state other objects care about. A spreadsheet cell feeds several charts; a stock price feeds a ticker, an alert service and a trading log; a student's quiz result feeds a progress dashboard and a study planner. The naive solution is to let the object call each interested party directly. That couples the object to every receiver: each new receiver means editing the object, and the object now depends on classes that often live in a higher layer, such as the user interface.

The Observer pattern removes this coupling. It defines a one-to-many dependency between objects so that when one object changes state, all of its dependents are notified and updated automatically.

## Participants

- **Subject**: knows its observers and provides operations to attach and detach them. Any number of observers may observe one subject.
- **Observer**: defines an updating interface, usually a single method such as `update()` or `onChanged(event)`.
- **ConcreteSubject**: stores the state of interest and sends a notification to its observers when that state changes.
- **ConcreteObserver**: keeps a reference to what it needs and implements the update interface to keep itself consistent with the subject.

## Collaboration

1. Observers register with the subject by calling `attach(observer)`.
2. When the subject's state changes, it calls `notifyObservers()`, which loops over the registered observers and calls `update` on each one.
3. Each observer reacts in its own way: a view repaints, a planner revises a plan, a scheduler creates a reminder.

The subject does not know the concrete classes of its observers. It only knows that they implement the observer interface. This is what allows the subject to sit in a lower layer than the observers.

## Push versus pull

In the **push** model the subject sends detailed information with the notification, for example an event object holding the old and new values. In the **pull** model the subject sends a minimal notification and observers ask the subject for the details they need. Push is convenient when all observers need the same data; pull avoids sending data that most observers ignore, but observers must then hold a reference to the subject.

## Consequences

- **Loose coupling**: subject and observers can vary independently and can belong to different layers.
- **Broadcast communication**: the subject does not care how many observers there are.
- **Unexpected updates**: a seemingly harmless change to the subject can trigger a cascade of updates. Observers should be cheap and should not throw exceptions that stop other observers from being notified.
- **Memory leaks**: an observer that is never detached keeps being referenced by the subject (the "lapsed listener" problem).

## Example

```java
public interface ProgressObserver {
    void onProgressChanged(ProgressEvent event);
}

public class ProgressTracker {
    private final List<ProgressObserver> observers = new ArrayList<>();

    public void attach(ProgressObserver o) { observers.add(o); }
    public void detach(ProgressObserver o) { observers.remove(o); }

    public void recordResult(GradeResult r) {
        // update mastery, then
        notifyObservers(new ProgressEvent(...));
    }

    private void notifyObservers(ProgressEvent e) {
        for (ProgressObserver o : observers) o.onProgressChanged(e);
    }
}
```

## Relation to other patterns

The Mediator pattern also decouples objects, but a mediator centralises the interaction logic, while observers each decide independently how to react. Java's event listeners (for example `ActionListener` in Swing) are an application of Observer.

## Review questions

1. Why does the subject depend only on the observer interface and not on concrete observers?
2. What is the difference between the push and the pull model?
3. Give one risk of the Observer pattern and explain how to reduce it.
