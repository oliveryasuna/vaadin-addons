# Vaadin Add-ons

A collection of Vaadin Flow add-ons that enhance the developer experience and provide additional functionality for modern web applications.

## 📦 Add-ons

### React Renderer
A powerful add-on that allows you to use React components as renderers in Vaadin components like `Grid`, `ComboBox`, `VirtualList`, and others.

**Features:**
- ✨ JSX Support with runtime transpilation
- ⚛️ Direct `React.createElement` support
- 🔄 Seamless data binding
- 🎯 Event handling with server-side functions
- 🏆 Full type safety with generics
- 🔧 Drop-in replacement for `LitRenderer`

[📚 Read the full documentation](./addons/react-renderer/addon/README.md)

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Vaadin Flow 24.0+
- Modern browser with ES6 support

### Building the Project

Clone the repository and build all add-ons:

```bash
git clone https://github.com/oliveryasuna/vaadin-addons.git
cd vaadin-addons
./gradlew build
```

### Running the Demo

To see the add-ons in action, run the demo application:

```bash
./gradlew {addon-name}:bootRun
```

Open your browser to `http://localhost:8080` to explore the examples.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Development Setup

1. Clone the repository
2. Import into your IDE as a Gradle project
3. Make your changes
4. Run tests: `./gradlew test`
5. Submit a pull request

## 📄 License

This project is licensed under the BSD-3-Clause License - see the [LICENSE](LICENSE) file for details.

## 🐛 Issues & Support

If you encounter any issues or have questions:

- [Open an issue](https://github.com/oliveryasuna/vaadin-addons/issues) on GitHub
- Check existing issues for solutions
- Include version numbers and error details when reporting bugs

## 🔗 Links

- [Vaadin Platform](https://vaadin.com/)
- [Vaadin Add-on Directory](https://vaadin.com/directory)
- [Vaadin Documentation](https://vaadin.com/docs)

---

**Author:** Oliver Yasuna  
**Copyright:** © 2025 Oliver Yasuna
