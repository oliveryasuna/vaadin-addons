package com.oliveryasuna.vaadin.reactrenderer;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.data.provider.DataGenerator;
import com.vaadin.flow.data.provider.DataKeyMapper;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.Rendering;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.internal.JsonSerializer;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.internal.StateTree;
import com.vaadin.flow.internal.nodefeature.ReturnChannelMap;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.shared.Registration;
import elemental.json.JsonArray;

import java.util.*;
import java.util.regex.Pattern;

/**
 * A {@link Renderer} that uses React to render given model objects in
 * components that support the JS renderer functions API.
 * <p>
 * This renderer allows you to create React-based renderers for Vaadin components
 * like {@code Grid}, {@code ComboBox}, and {@code VirtualList}. It supports two
 * rendering modes:
 * <ul>
 *   <li>
 *     <strong>{@code React.createElement} mode</strong> -
 *     Use {@link #of(String)} for direct {@code React.createElement} calls in
 *     your template expression
 *   </li>
 *   <li>
 *     <strong>JSX mode</strong> -
 *     Use {@link #jsx(String)} for JSX syntax that gets transpiled at runtime
 *     using Babel
 *   </li>
 * </ul>
 * <p>
 * The renderer integrates with Vaadin's data binding system, allowing you to:
 * <ul>
 *   <li>
 *     Bind model properties using {@link #withProperty(String, ValueProvider)}
 *   </li>
 *   <li>
 *     Handle client-side events using
 *     {@link #withFunction(String, SerializableConsumer)} or
 *     {@link #withFunction(String, SerializableBiConsumer)}
 *   </li>
 * </ul>
 * <p>
 * Example usage with JSX:
 * <pre>{@code
 * ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
 *     "({item, handleClick}) => <div onClick={handleClick}>{item.name}</div>"
 * )
 * .withProperty("name", Person::getName)
 * .withFunction("handleClick", person -> System.out.println("Clicked: " + person.getName()));
 * }</pre>
 * <p>
 * Example usage with {@code React.createElement}:
 * <pre>{@code
 * ReactRenderer<Person> renderer = ReactRenderer.<Person>of(
 *     "({item, handleClick}) => React.createElement('div', {onClick: handleClick}, item.name)"
 * )
 * .withProperty("name", Person::getName)
 * .withFunction("handleClick", person -> System.out.println("Clicked: " + person.getName()));
 * }</pre>
 * <p>
 * Based on {@link com.vaadin.flow.data.renderer.LitRenderer}.
 *
 * @param <SOURCE>
 *     The type of the model object used inside the template
 *
 * @see #jsx(String)
 * @see #of(String)
 * @see #withProperty(String, ValueProvider)
 * @see #withFunction(String, SerializableConsumer)
 * @see #withFunction(String, SerializableBiConsumer)
 * @see <a href="https://react.dev/learn/writing-markup-with-jsx">React JSX Documentation</a>
 * @see com.vaadin.flow.data.renderer.LitRenderer
 */
@JsModule("./react-renderer.ts")
@NpmPackage(value = "@babel/standalone", version = "7.27.1")
@NpmPackage(value = "@types/babel__standalone", version = "7.1.9", dev = true)
public class ReactRenderer<SOURCE> extends Renderer<SOURCE> {

    // Static fields
    //--------------------------------------------------

    private static final Pattern ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    // Fields
    //--------------------------------------------------

    protected final String templateExpression;
    protected final boolean jsx;

    protected final String propertyNamespace;

    protected final Map<String, ValueProvider<SOURCE, ?>> valueProviders;
    protected final Map<String, SerializableBiConsumer<SOURCE, JsonArray>> clientCallables;

    // Constructors
    //--------------------------------------------------

    protected ReactRenderer(final String templateExpression, final boolean jsx) {
        super();

        this.templateExpression = templateExpression;
        this.jsx = jsx;

        this.propertyNamespace = "rr_%s_".formatted(UUID.randomUUID().toString().replace("-", "").substring(0, 16));

        this.valueProviders = new HashMap<>();
        this.clientCallables = new HashMap<>();
    }

    // Static methods
    //--------------------------------------------------

    /**
     * Creates a new {@link ReactRenderer} with the specified template
     * expression and transpilation mode.
     * <p>
     * The template expression should be a JavaScript function that takes props
     * and returns a React element.
     * When {@code transpile} is {@code true}, the template will be processed as
     * JSX and transpiled using Babel.
     * When {@code false}, the template should use direct
     * {@code React.createElement} calls.
     *
     * @param templateExpression
     *     The JavaScript template expression that defines how to render each
     *     item, not {@code null}
     * @param transpile
     *     {@code true} to enable JSX transpilation using Babel, {@code false}
     *     to use direct {@code React.createElement} calls
     * @param <SOURCE>
     *     The type of the model object used inside the template
     * @return
     *     A new {@link ReactRenderer} instance
     * @throws NullPointerException
     *     If {@code templateExpression} is {@code null}
     *
     * @see #of(String)
     * @see #jsx(String)
     */
    public static <SOURCE> ReactRenderer<SOURCE> of(final String templateExpression, final boolean transpile) {
        Objects.requireNonNull(templateExpression);

        return new ReactRenderer<>(templateExpression, transpile);
    }

    /**
     * Creates a new {@link ReactRenderer} with the specified template
     * expression without JSX transpilation.
     * <p>
     * This is a convenience method equivalent to
     * {@link #of(String, boolean) of(templateExpression, false)}.
     * The template expression should use direct {@code React.createElement}
     * calls.
     *
     * @param templateExpression
     *     The JavaScript template expression that defines how to render each item using
     *     {@code React.createElement} calls, not {@code null}
     * @param <SOURCE>
     *     The type of the model object used inside the template
     * @return
     *     A new {@link ReactRenderer} instance without JSX transpilation
     * @throws NullPointerException
     *     If {@code templateExpression} is {@code null}
     *
     * @see #of(String, boolean)
     * @see #jsx(String)
     */
    public static <SOURCE> ReactRenderer<SOURCE> of(final String templateExpression) {
        return of(templateExpression, false);
    }

    /**
     * Creates a new {@link ReactRenderer} with JSX transpilation enabled.
     * <p>
     * This is a convenience method equivalent to
     * {@link #of(String, boolean) of(templateExpression, true)}.
     * The template expression should use JSX syntax, which will be transpiled
     * at runtime using Babel.
     *
     * @param templateExpression
     *     The JavaScript template expression using JSX syntax that defines how
     *     to render each item, not {@code null}
     * @param <SOURCE>
     *     The type of the model object used inside the template
     * @return
     *     A new instance with JSX transpilation enabled
     * @throws NullPointerException
     *     If {@code templateExpression} is {@code null}
     *
     * @see #of(String)
     * @see #of(String, boolean)
     */
    public static <SOURCE> ReactRenderer<SOURCE> jsx(final String templateExpression) {
        return of(templateExpression, true);
    }

    // Methods
    //--------------------------------------------------

    /**
     * Makes a property available to the template expression.
     * <p>
     * Each property is referenced inside the template by using the
     * {@code item.property} syntax.
     * <p>
     * Example usage:
     * <pre>{@code
     * // For a Person class with getName() and getAge() methods
     * ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
     *     "({item}) => <div>{item.name} - Age: {item.age}</div>"
     * )
     * .withProperty("name", Person::getName)
     * .withProperty("age", Person::getAge);
     * 
     * // You can also use computed properties
     * renderer.withProperty("displayName", person -> person.getFirstName() + " " + person.getLastName());
     * }</pre>
     *
     * @param property
     *     The name of the property used inside the template expression, not
     *     {@code null}
     * @param provider
     *     Provider of the actual value for the property, not {@code null}
     * @return
     *     This instance for method chaining
     * @throws NullPointerException
     *    If {@code property} or {@code provider} is {@code null}
     */
    public ReactRenderer<SOURCE> withProperty(final String property, final ValueProvider<SOURCE, ?> provider) {
        Objects.requireNonNull(property);
        Objects.requireNonNull(provider);

        valueProviders.put(property, provider);

        return this;
    }

    /**
     * Registers a client-side callable function that takes only the model item
     * as parameter.
     * <p>
     * This is a convenience method for
     * {@link #withFunction(String, SerializableBiConsumer)} when you don't need
     * access to client-side arguments.
     * The function can be invoked from the template by using the function name
     * as a prop.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Simple click handler
     * ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
     *     "({item, onEdit, onDelete}) => " +
     *     "<div>" +
     *     "  <span>{item.name}</span>" +
     *     "  <button onClick={onEdit}>Edit</button>" +
     *     "  <button onClick={onDelete}>Delete</button>" +
     *     "</div>"
     * )
     * .withProperty("name", Person::getName)
     * .withFunction("onEdit", person -> editPerson(person))
     * .withFunction("onDelete", person -> deletePerson(person));
     * }</pre>
     *
     * @param functionName
     *     The name of the function that can be called from the client-side
     *     template, must be alphanumeric, not {@code null}
     * @param handler
     *     The server-side handler that will be called when the client invokes
     *     this function, not {@code null}
     * @return
     *     This instance for method chaining
     * @throws NullPointerException
     *     If {@code functionName} or {@code handler} is {@code null}
     * @throws IllegalArgumentException
     *     If {@code functionName} is not alphanumeric
     *
     * @see #withFunction(String, SerializableBiConsumer)
     */
    public ReactRenderer<SOURCE> withFunction(final String functionName, final SerializableConsumer<SOURCE> handler) {
        return withFunction(functionName, (item, ignored) -> {
            handler.accept(item);
        });
    }

    /**
     * Registers a client-side callable function that can receive arguments from
     * the client.
     * <p>
     * This method allows you to register functions that can be called from the
     * React template and can access both the model item and any arguments
     * passed from the client-side.
     * The function can be invoked from the template by using the function name
     * as a prop.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Handler that receives client arguments
     * ReactRenderer<Person> renderer = ReactRenderer.<Person>jsx(
     *     "({item, onSort, onFilter}) => " +
     *     "<div>" +
     *     "  <span>{item.name}</span>" +
     *     "  <button onClick={() => onSort('name', 'asc')}>Sort Name ↑</button>" +
     *     "  <button onClick={() => onSort('name', 'desc')}>Sort Name ↓</button>" +
     *     "  <input onChange={(e) => onFilter('name', e.target.value)} placeholder='Filter by name' />" +
     *     "</div>"
     * )
     * .withProperty("name", Person::getName)
     * .withFunction("onSort", (person, args) -> {
     *     String field = args.getString(0);
     *     String direction = args.getString(1);
     *     handleSort(field, direction);
     * })
     * .withFunction("onFilter", (person, args) -> {
     *     String field = args.getString(0);
     *     String value = args.getString(1);
     *     handleFilter(field, value);
     * });
     * }</pre>
     *
     * @param functionName
     *     The name of the function that can be called from the client-side
     *     template, must be alphanumeric, not {@code null}
     * @param handler
     *     The server-side handler that will be called when the client invokes
     *     this function.
     *     Receives the model item and a {@link JsonArray} of arguments from the
     *     client, not {@code null}
     * @return
     *     This instance for method chaining
     * @throws NullPointerException
     *     If {@code functionName} or {@code handler} is {@code null}
     * @throws IllegalArgumentException
     *     If {@code functionName} is not alphanumeric
     *
     * @see #withFunction(String, SerializableConsumer)
     */
    public ReactRenderer<SOURCE> withFunction(final String functionName, final SerializableBiConsumer<SOURCE, JsonArray> handler) {
        Objects.requireNonNull(functionName);
        Objects.requireNonNull(handler);

        if(!ALPHANUMERIC_PATTERN.matcher(functionName).matches()) {
            throw new IllegalArgumentException("Function name must be alphanumeric: " + functionName);
        }

        clientCallables.put(functionName, handler);

        return this;
    }

    @Override
    public Rendering<SOURCE> render(final Element container, final DataKeyMapper<SOURCE> keyMapper, final String rendererName) {
        final DataGenerator<SOURCE> dataGenerator = createDataGenerator();
        final Registration registration = createJsRendererFunction(container, keyMapper, rendererName);

        return new Rendering<>() {
            @Override
            public Optional<DataGenerator<SOURCE>> getDataGenerator() {
                return Optional.of(dataGenerator);
            }

            @Override
            public Registration getRegistration() {
                return registration;
            }
        };
    }

    private DataGenerator<SOURCE> createDataGenerator() {
        return (item, jsonObject) -> {
            for(final Map.Entry<String, ValueProvider<SOURCE, ?>> valueProvider : valueProviders.entrySet()) {
                final String key = valueProvider.getKey();
                final ValueProvider<SOURCE, ?> provider = valueProvider.getValue();

                jsonObject.put(
                        // Prefix with renderer-specific namespace
                        propertyNamespace + key,
                        JsonSerializer.toJson(provider.apply(item))
                );
            }
        };
    }

    private Registration createJsRendererFunction(final Element container, final DataKeyMapper<SOURCE> keyMapper, final String rendererName) {
        final ReturnChannelRegistration returnChannel = container.getNode().getFeature(ReturnChannelMap.class).registerChannel(arguments -> {
            // Invoked when the client calls one of the client callables
            final String handlerName = arguments.getString(0);
            final String itemKey = arguments.getString(1);
            final JsonArray args = arguments.getArray(2);

            final SerializableBiConsumer<SOURCE, JsonArray> handler = clientCallables.get(handlerName);
            final SOURCE item = keyMapper.get(itemKey);
            if(item != null) {
                handler.accept(item, args);
            }
        });

        final JsonArray clientCallablesArray = JsonUtils.listToJson(new ArrayList<>(clientCallables.keySet()));

        final List<Registration> registrations = new ArrayList<>();

        // Attach listener for when component gets reattached
        registrations.add(container.addAttachListener(event -> {
            setElementRenderer(container, rendererName, returnChannel, clientCallablesArray);
        }));

        // Set renderer initially if already attached
        if(container.getNode().isAttached()) {
            setElementRenderer(container, rendererName, returnChannel, clientCallablesArray);
        }

        // Clean up when renderer is unregistered
        registrations.add(() -> {
            container.executeJs("window.Vaadin.unsetReactRenderer(this, $0, $1)", rendererName, propertyNamespace);
        });

        return () -> {
            for(final Registration registration : registrations) {
                registration.remove();
            }
        };
    }

    private void setElementRenderer(
            final Element container,
            final String rendererName,
            final ReturnChannelRegistration returnChannel,
            final JsonArray clientCallablesArray
    ) {
        assert container.getNode().isAttached() : "Container must be attached";

        final String appId = getElementUI(container).getInternals().getAppId();
        container.executeJs(
                "window.Vaadin.setReactRenderer(this, $0, $1, $2, $3, $4, $5, $6)",
                rendererName,
                templateExpression,
                jsx,
                returnChannel,
                clientCallablesArray,
                propertyNamespace,
                appId
        );
    }

    private UI getElementUI(final Element element) {
        return ((StateTree)element.getNode().getOwner()).getUI();
    }

    // Getters/setters
    //--------------------------------------------------

    /**
     * Returns an unmodifiable view of the value providers registered with this
     * renderer.
     * <p>
     * This method provides access to all property bindings that have been
     * registered using {@link #withProperty(String, ValueProvider)}.
     * The returned map contains the property names as keys and their
     * corresponding {@link ValueProvider} instances as values.
     *
     * @return
     *     An unmodifiable map of property names to their corresponding value
     *     providers
     *
     * @see #withProperty(String, ValueProvider)
     */
    public Map<String, ValueProvider<SOURCE, ?>> getValueProviders() {
        return Collections.unmodifiableMap(valueProviders);
    }

}
