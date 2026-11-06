# Week 7 - Factory Method, Adapter and Facade

## Factory Method

**Intent.** Define an interface for creating an object, but let subclasses decide which class to instantiate.

**Problem.** A quiz generator receives question specifications such as `{"type": "MULTIPLE_CHOICE", "options": [...], "answer": "B"}`. Each type has different validation rules: a multiple-choice question needs three to five distinct options and a valid answer, a true/false question needs a boolean answer, and a short-answer question needs a model answer. Creating questions with a `switch` on the type couples the generator to every concrete question class and repeats the validation wherever questions are created.

**Structure.** An abstract **Creator** (`QuestionFactory`) declares the factory method `createQuestion(spec)` and often provides a template operation `build(spec)` that runs the checks common to all products before calling the factory method. **Concrete creators** (`MultipleChoiceQuestionFactory`, `TrueFalseQuestionFactory`, `ShortAnswerQuestionFactory`) override the factory method and return a **concrete product** (`MultipleChoiceQuestion`, ...). Clients know only the creator and the abstract **Product** (`Question`).

**Consequences.** New product types are added by adding one product class and one creator. Clients that use products polymorphically (for example a grader calling `question.grade(response)`) do not change.

## Adapter

**Intent.** Convert the interface of a class into another interface clients expect. Adapter lets classes work together that could not otherwise because of incompatible interfaces.

**Problem.** An application wants to call large language models from different vendors. One vendor's SDK offers `generateContent(request)` with "parts" and "candidates"; another offers `createChatCompletion(messages)` with "choices". The application should depend on neither.

**Structure.** The **Target** is the interface the client wants (`LLMClient` with `generate()` and `generateStructured()`). The **Adaptee** is the existing incompatible class (`GeminiSdkClient`, `OpenAISdkClient`). The **Adapter** (`GeminiLLMAdapter`, `OpenAILLMAdapter`) implements the target and translates each call into calls on the adaptee, including the conversion of errors and token counts.

**Object adapter versus class adapter.** An object adapter holds a reference to the adaptee (composition); a class adapter inherits from it. Java has single inheritance of classes, so object adapters are the usual choice.

**Consequences.** Switching vendor becomes a configuration change; tests can supply a fake implementation of the target interface.

## Facade

**Intent.** Provide a unified interface to a set of interfaces in a subsystem. Facade defines a higher-level interface that makes the subsystem easier to use.

**Problem.** A learning application has a graphical interface and a command-line interface. Each feature involves several subsystems: services, an AI agent, repositories and a retrieval index. If both front ends called these directly, the orchestration code would be written twice and the user interface would depend on internal classes.

**Structure.** The **Facade** (`LearningFacade`) knows which subsystem classes are responsible for a request and delegates to them. **Subsystem classes** do the work and do not know about the facade. **Clients** (all GUI views and the CLI) call only the facade.

**Consequences.** Front ends are shielded from subsystem changes; a third front end can reuse the facade. The facade's interface can become large, so it should stay thin and only delegate.

## Comparing the three

| Pattern | Kind | Main idea |
|---|---|---|
| Factory Method | creational | subclasses decide which product to create |
| Adapter | structural | make an existing class fit an interface clients expect |
| Facade | structural | one simple entry point in front of a complex subsystem |

## Review questions

1. In Factory Method, what is the role of the template operation `build(spec)`?
2. Why is an object adapter usually preferred over a class adapter in Java?
3. How does a facade help a project that must offer both a GUI and a CLI?
