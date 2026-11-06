# Week 6 - Strategy and State

## The Strategy pattern

### Problem

A class often needs one of several algorithms for the same task: sorting with different orders, computing shipping with different carriers, or scheduling study time differently depending on how close an exam is. Putting all variants into one method produces a long conditional that grows every time a variant is added, and each variant becomes hard to test on its own.

### Solution

Strategy defines a family of algorithms, encapsulates each one in its own class, and makes them interchangeable. The client works with an abstract strategy interface and can switch implementations at run time.

### Participants

- **Strategy**: the common interface of all supported algorithms, for example `buildSchedule(request, priorities)`.
- **ConcreteStrategy**: implements one algorithm, for example `ExamCramStrategy`, `SpacedRepetitionStrategy` and `BalancedReviewStrategy`.
- **Context**: is configured with a strategy object, keeps a reference to it and delegates the work to it. The context may choose the strategy itself from the input (for example, fewer than seven days before the exam selects cramming).

### Consequences

- New algorithms are added without changing the context (open-closed principle).
- Each algorithm can be unit-tested in isolation with fixed input.
- Clients must know enough about the strategies to choose one, unless the context chooses.
- A small number of extra classes and objects.

## The State pattern

### Problem

An object behaves differently depending on its internal state, and the state changes at run time. Example: an adaptive practice session at the foundation level asks true/false questions and moves up after three correct answers in a row; at the advanced level it asks short-answer questions and moves down after two wrong answers. Encoding this with `if (level == ...)` everywhere scatters the transition rules over many methods.

### Solution

State allows an object to alter its behaviour when its internal state changes; the object appears to change its class. Each state is a class that implements a common state interface and knows the transitions out of itself.

### Participants

- **Context**: holds the current state object and delegates state-specific requests to it (`recordOutcome()` calls `state.onResult(this, result)`).
- **State**: interface for the behaviour associated with one state (`getLevel()`, `onResult()`, `preferredQuestionTypes()`).
- **ConcreteState**: each subclass implements the behaviour of one state, for example `FoundationState`, `IntermediateState` and `AdvancedState`, and decides when to call `context.setState(next)`.

### Consequences

- State-specific behaviour is localised and transitions are explicit.
- Adding a state means adding one class.
- The number of classes grows with the number of states.

## Strategy versus State

The two patterns have almost the same class diagram: a context delegating to an interface with several implementations. The difference is intent and who changes the object:

| | Strategy | State |
|---|---|---|
| What varies | the algorithm used for a task | the behaviour as a whole, as the object's situation changes |
| Who switches | usually the client or context, from outside | the states themselves, as part of their behaviour |
| Do implementations know each other? | no | yes, a state knows its successor states |

## Review questions

1. Why is a long `switch` on an algorithm type a sign that Strategy may help?
2. In the State pattern, which participant decides that the session should move to a harder level?
3. A study planner picks spaced repetition when the exam is more than three weeks away. Is the planner using Strategy or State? Explain.
