# Credit Bureau Configuration Design Pattern

## Overview

This package implements a flexible configuration system for credit bureaus using a combination of design patterns:

1. **Strategy Pattern**: The parent class `CreditBureauConfigurationParam` defines a common interface for all credit bureau configurations, while concrete child classes implement specific configurations.

2. **Map-based Dynamic Properties**: The parent class uses a Map to store configuration parameters, allowing for dynamic addition of properties without modifying the class structure.

3. **Factory Method Pattern**: Both the parent class and child classes provide factory methods to create instances with specific configurations.

## Class Structure

### Parent Class: `CreditBureauConfigurationParam`

The parent class provides:
- Common fields for all credit bureau configurations (organization IDs)
- A Map to store dynamic configuration parameters
- Methods to get and set parameters by key
- A method to get all parameters
- A factory method to create basic instances

### Example Child Class: `ExampleCreditBureauConfig`

The example child class demonstrates:
- How to extend the parent class
- How to define constants for parameter keys
- How to implement type-safe getters and setters for specific parameters
- How to create a factory method for fully configured instances

## Usage Examples

The `CreditBureauConfigExample` class demonstrates different ways to use these classes:

1. Using the parent class directly for a generic credit bureau
2. Using a specific child class with type-safe getters and setters
3. Adding custom parameters to a child class instance
4. Using the factory method to create a fully configured instance
5. Creating another type of configuration directly with the parent class

## How to Create a New Credit Bureau Configuration

To create a configuration for a new credit bureau:

1. Create a new class that extends `CreditBureauConfigurationParam`
2. Define constants for parameter keys
3. Implement constructors that call the parent constructors
4. Implement type-safe getters and setters for specific parameters
5. Implement a factory method if needed

Example:

```java
public class NewCreditBureauConfig extends CreditBureauConfigurationParam {
    
    // Constants for parameter keys
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    
    // Constructors
    public NewCreditBureauConfig() {
        super();
    }
    
    public NewCreditBureauConfig(long creditBureauOrganizationParamId, long creditBureauOrganizationId) {
        super(creditBureauOrganizationParamId, creditBureauOrganizationId);
    }
    
    // Type-safe getters and setters
    public String getUsername() {
        return getParam(USERNAME);
    }
    
    public NewCreditBureauConfig setUsername(String username) {
        setParam(USERNAME, username);
        return this;
    }
    
    public String getPassword() {
        return getParam(PASSWORD);
    }
    
    public NewCreditBureauConfig setPassword(String password) {
        setParam(PASSWORD, password);
        return this;
    }
    
    // Factory method
    public static NewCreditBureauConfig instance(
            final long creditBureauOrganizationId, 
            final long creditBureauOrganizationParamId,
            final String username,
            final String password) {
        
        return new NewCreditBureauConfig(creditBureauOrganizationParamId, creditBureauOrganizationId)
                .setUsername(username)
                .setPassword(password);
    }
}
```

## Benefits of This Approach

1. **Flexibility**: Can handle any number of configuration parameters with different names
2. **Type Safety**: Child classes can provide type-safe access to their specific parameters
3. **Extensibility**: Easy to add new credit bureau configurations without modifying existing code
4. **Consistency**: Common fields and methods are defined in the parent class
5. **Encapsulation**: Implementation details are hidden behind a clean interface