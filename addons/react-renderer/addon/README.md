# Vaadin React Renderer

A Vaadin Flow add-on that allows you to use React components as renderers in
`Grid`, `ComboBox`, `VirtualList`, and other components that support the JS
renderer functions API.

[Is your Grid too slow?](https://vaadin.com/blog/using-the-right-r)

## Features

- ✨ **JSX Support**:
  Write your renderers using familiar JSX syntax with runtime transpilation via
  Babel
- ⚛️ **`React.createElement` Support**:
  Use direct `React.createElement` calls for better performance
- 🔄 **Data Binding**:
  Seamlessly bind model properties to your React components
- 🎯 **Event Handling**:
  Register server-side functions callable from your React templates
- 🏆 **Type Safety**:
  Full Java type safety with generic support
- 🔧 **Easy Integration**:
  Drop-in replacement for `LitRenderer`

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.vaadin.addons.oliveryasuna</groupId>
    <artifactId>react-renderer</artifactId>
    <version>1.0.0</version>
</dependency>
```

Or for Gradle projects:

```gradle
implementation 'org.vaadin.addons.oliveryasuna:react-renderer:1.0.0'
```

## Quick Start

### Basic Usage with JSX

```java
// Create a renderer with JSX syntax
ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
    "({item, onEdit}) => " +
    "<div className='person-card'>" +
    "  <h3>{item.name}</h3>" +
    "  <p>Age: {item.age}</p>" +
    "  <button onClick={onEdit}>Edit</button>" +
    "</div>"
)
.withProperty("name", Person::getName)
.withProperty("age", Person::getAge)
.withFunction("onEdit", person -> editPerson(person));

// Use it in a Grid
Grid<Person> grid = new Grid<>();
grid.addColumn(renderer).setHeader("Person Details");
```

### Using `React.createElement`

```java
// Create a renderer with React.createElement calls
ReactRenderer<Person> renderer = ReactRenderer.<Person>of(
    "({item, onEdit}) => " +
    "React.createElement('div', {className: 'person-card'}, " +
    "  React.createElement('h3', null, item.name), " +
    "  React.createElement('p', null, 'Age: ' + item.age), " +
    "  React.createElement('button', {onClick: onEdit}, 'Edit')" +
    ")"
)
.withProperty("name", Person::getName)
.withProperty("age", Person::getAge)
.withFunction("onEdit", person -> editPerson(person));
```

## API Reference

### Creating Renderers

#### `ReactRenderer.jsx(String templateExpression)`
Creates a renderer with JSX transpilation enabled.

```java
ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
    "({item}) => <div>{item.name}</div>"
);
```

#### `ReactRenderer.of(String templateExpression)`
Creates a renderer using` React.createElement` calls (no transpilation).

```java
ReactRenderer<Person> renderer = ReactRenderer.<Person>of(
    "({item}) => React.createElement('div', null, item.name)"
);
```

#### `ReactRenderer.of(String templateExpression, boolean transpile)`
Creates a renderer with explicit transpilation control.

```java
// With transpilation (same as jsx())
ReactRenderer<Person> renderer = ReactRenderer.of(template, true);

// Without transpilation (same as of())
ReactRenderer<Person> renderer = ReactRenderer.of(template, false);
```

### Binding Data

#### `withProperty(String property, ValueProvider<SOURCE, ?> provider)`
Binds model data to template properties.

```java
renderer
    .withProperty("name", Person::getName)
    .withProperty("email", Person::getEmail)
    .withProperty("displayName", person ->
        person.getFirstName() + " " + person.getLastName())
    .withProperty("isActive", Person::isActive);
```

### Handling Events

#### `withFunction(String functionName, SerializableConsumer<SOURCE> handler)`
Registers a simple event handler that only receives the model item.

```java
renderer
    .withFunction("onEdit", person -> openEditDialog(person))
    .withFunction("onDelete", person -> deletePerson(person))
    .withFunction("onToggleStatus", person -> toggleStatus(person));
```

#### `withFunction(String functionName, SerializableBiConsumer<SOURCE, JsonArray> handler)`
Registers an event handler that can receive arguments from the client.

```java
renderer.withFunction("onSort", (person, args) -> {
    String field = args.getString(0);
    String direction = args.getString(1);
    applySorting(field, direction);
});
```

## Examples

### Grid with Complex Rendering

```java
public class PersonGridExample extends VerticalLayout {

    public PersonGridExample() {
        Grid<Person> grid = new Grid<>();

        // Create a comprehensive renderer
        ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
            "({item, onEdit, onDelete, onSendEmail, onToggleStatus}) => " +
            "<div className='person-row'>" +
            "  <div className='person-info'>" +
            "    <img src={item.avatarUrl} alt='Avatar' className='avatar' />" +
            "    <div>" +
            "      <h4>{item.displayName}</h4>" +
            "      <p className='email'>{item.email}</p>" +
            "      <span className={'status ' + (item.active ? 'active' : 'inactive')}>" +
            "        {item.active ? 'Active' : 'Inactive'}" +
            "      </span>" +
            "    </div>" +
            "  </div>" +
            "  <div className='actions'>" +
            "    <button onClick={onEdit} className='btn-primary'>Edit</button>" +
            "    <button onClick={onSendEmail} className='btn-secondary'>Email</button>" +
            "    <button onClick={onToggleStatus} " +
            "            className={item.active ? 'btn-warning' : 'btn-success'}>" +
            "      {item.active ? 'Deactivate' : 'Activate'}" +
            "    </button>" +
            "    <button onClick={onDelete} className='btn-danger'>Delete</button>" +
            "  </div>" +
            "</div>"
        )
        // Bind all necessary properties
        .withProperty("displayName", person ->
            person.getFirstName() + " " + person.getLastName())
        .withProperty("email", Person::getEmail)
        .withProperty("active", Person::isActive)
        .withProperty("avatarUrl", person ->
            "/avatars/" + person.getId() + ".jpg")

        // Register event handlers
        .withFunction("onEdit", this::editPerson)
        .withFunction("onDelete", this::deletePerson)
        .withFunction("onSendEmail", this::sendEmail)
        .withFunction("onToggleStatus", this::togglePersonStatus);

        grid.addColumn(renderer).setHeader("Person Details").setAutoWidth(true);
        grid.setItems(getPersons());

        add(grid);
    }

    private void editPerson(Person person) {
        // Open edit dialog
        new PersonEditDialog(person).open();
    }

    private void deletePerson(Person person) {
        // Show confirmation and delete
        ConfirmDialog.createQuestion()
            .withText("Delete " + person.getFirstName() + "?")
            .withOkButton(() -> personService.delete(person))
            .withCancelButton()
            .open();
    }

    private void sendEmail(Person person) {
        // Open email composer
        new EmailDialog(person.getEmail()).open();
    }

    private void togglePersonStatus(Person person) {
        person.setActive(!person.isActive());
        personService.save(person);
        // Refresh grid if needed
    }
}
```

### ComboBox with Custom Renderer

```java
public class PersonComboBox extends ComboBox<Person> {

    public PersonComboBox() {
        super("Select Person");

        ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
            "({item}) => " +
            "<div className='person-option'>" +
            "  <img src={item.avatarUrl} className='small-avatar' />" +
            "  <div>" +
            "    <div className='name'>{item.displayName}</div>" +
            "    <div className='role'>{item.role}</div>" +
            "  </div>" +
            "</div>"
        )
        .withProperty("displayName", person ->
            person.getFirstName() + " " + person.getLastName())
        .withProperty("role", Person::getRole)
        .withProperty("avatarUrl", person ->
            "/avatars/" + person.getId() + ".jpg");

        setRenderer(renderer);
        setItems(personService.findAll());
    }
}
```

### Advanced Event Handling

```java
// Renderer that handles sorting and filtering
ReactRenderer<Person> tableRenderer = ReactRenderer.<Person>jsx(
    "({item, onSort, onFilter, onAction}) => " +
    "<div className='table-row'>" +
    "  <div className='cell'>" +
    "    <span>{item.name}</span>" +
    "    <button onClick={() => onSort('name', 'asc')} title='Sort A-Z'>↑</button>" +
    "    <button onClick={() => onSort('name', 'desc')} title='Sort Z-A'>↓</button>" +
    "  </div>" +
    "  <div className='cell'>" +
    "    <input " +
    "      type='text' " +
    "      placeholder='Filter...' " +
    "      onChange={(e) => onFilter('name', e.target.value)} " +
    "    />" +
    "  </div>" +
    "  <div className='cell'>" +
    "    <select onChange={(e) => onAction(e.target.value)}>" +
    "      <option value=''>Choose action...</option>" +
    "      <option value='edit'>Edit</option>" +
    "      <option value='duplicate'>Duplicate</option>" +
    "      <option value='delete'>Delete</option>" +
    "    </select>" +
    "  </div>" +
    "</div>"
)
.withProperty("name", Person::getName)
.withFunction("onSort", (person, args) -> {
    String field = args.getString(0);
    String direction = args.getString(1);
    handleSort(field, direction);
})
.withFunction("onFilter", (person, args) -> {
    String field = args.getString(0);
    String value = args.getString(1);
    handleFilter(field, value);
})
.withFunction("onAction", (person, args) -> {
    String action = args.getString(0);
    handleAction(person, action);
});
```

## Migration from LitRenderer

`ReactRenderer` can be used as a drop-in replacement for `LitRenderer` in most
cases:

```java
// Old LitRenderer code
LitRenderer<Person> litRenderer = LitRenderer.<Person>of(
    "<div>${item.name}</div>"
)
.withProperty("name", Person::getName);

// New ReactRenderer equivalent
ReactRenderer<Person> reactRenderer = ReactRenderer.<Person>jsx(
    "({item}) => <div>{item.name}</div>"
)
.withProperty("name", Person::getName);
```

## Requirements

- Vaadin Flow 24.0+
- Java 17+
- Modern browser with ES6 support

## Dependencies

The add-on automatically includes:
- React 18.x (bundled)
- Babel Standalone 7.27.1 (for JSX transpilation)

## Browser Compatibility

- Chrome 70+
- Firefox 65+
- Safari 12+
- Edge 79+

## License

This project is licensed under BSD-3-Clause - see the
[LICENSE](https://github.com/oliveryasuna/vaadin-react-renderer/blob/main/LICENSE)
file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

If you encounter any issues or have questions, please
[open an issue](https://github.com/oliveryasuna/vaadin-react-renderer/issues) on
GitHub.
