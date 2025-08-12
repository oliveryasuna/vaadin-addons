import React from 'react';
import {createRoot, Root} from 'react-dom/client';
import * as Babel from '@babel/standalone';

type RenderRoot = HTMLElement & {__reactRenderer?: Renderer; __reactRoot?: Root;};
type ItemModel = {item: any; index: number;}
type Renderer = ((root: RenderRoot, rendererOwner: HTMLElement, model: ItemModel) => void) & {__rendererId?: string;};
type Component = HTMLElement & Record<string, (Renderer | undefined)>;

const _window = window as any;
_window.Vaadin = _window.Vaadin || {};

_window.Vaadin.setReactRenderer = (
    component: Component,
    rendererName: string,
    templateExpression: string,
    transpile: boolean,
    returnChannel: (name: string, itemKey: string, args: any[]) => void,
    clientCallables: string[],
    propertyNamespace: string,
    appId: string
): void => {
    const callablesCreator = (itemKey: string): Record<string, ((...args: any[]) => void)> =>
        clientCallables.reduce(
            (acc: Record<string, ((...args: any[]) => void)>, clientCallable: string): Record<string, ((...args: any[]) => void)> => ({
                ...acc,
                [clientCallable]: (...args: any[]): void => {
                    if(itemKey === undefined) {
                        return;
                    }
                    returnChannel(clientCallable, itemKey, (args[0] instanceof Event ? [] : [...args]));
                }
            }),
            ({} as Record<string, ((...args: any[]) => void)>)
        );

    // Create a React component factory function from the template expression
    const createReactComponent = (templateExpression: string, transpile: boolean): ((item: any, index: number) => React.ReactNode) => {
        try {
            if(transpile) {
                // The template expression should be a function that returns JSX
                // E.g., "({item, index, handleClick}) => <div onClick={handleClick}>{item.name}</div>"

                // Transform JSX to React.createElement calls
                const result = Babel.transform(templateExpression, {
                    presets: [['react', {runtime: 'classic'}]],
                    plugins: [],
                    filename: 'template.jsx'
                });

                if(!result || !result.code) {
                    throw (new Error('Babel transformation returned empty result'));
                }

                // Create the component function
                return (new Function('React', `return ${result.code}`))(React);
            } else {
                // The template expression should be a function that returns JSX
                // E.g., "({item, index, handleClick}) => React.createElement('div', {onClick: handleClick}, item.name)"

                return (new Function('React', `return ${templateExpression}`))(React);
            }
        } catch(err: unknown) {
            console.error('Error creating React component from template:', err);

            return ({item: _item}: {item: any}) => React.createElement('div', {}, 'Render Error');
        }
    };

    const ReactComponent = createReactComponent(templateExpression, transpile);

    const renderFunction = (root: RenderRoot, model: ItemModel, itemKey: string): void => {
        const {item, index} = model;
        const callables: Record<string, ((...args: any[]) => void)> = callablesCreator(itemKey);

        // Create a React root if it doesn't exist
        if(!root.__reactRoot) {
            root.__reactRoot = createRoot(root);
        }

        // Render the React component
        const element = React.createElement(
            ReactComponent,
            {item: item, index: index, appId: appId, itemKey: itemKey, model: model, ...callables}
        );
        root.__reactRoot.render(element)
    };

    const renderer: Renderer = (root: RenderRoot, _: HTMLElement, model: ItemModel): void => {
        const {item} = model;

        // Clean up the root element if it was used by a different renderer
        if(root.__reactRenderer !== renderer) {
            // Unmount the previous React root if it exists
            if(root.__reactRoot) {
                root.__reactRoot.unmount();
                delete root.__reactRoot;
            }
            root.innerHTML = '';
            root.__reactRenderer = renderer;
        }

        // Map item properties with namespace prefix removed
        const mappedItem: Record<string, any> = {};
        for(const key in item) {
            if(!key.startsWith(propertyNamespace)) {
                continue;
            }
            mappedItem[key.replace(propertyNamespace, '')] = item[key];
        }

        renderFunction(root, {...model, item: mappedItem}, item.key);
    };

    renderer.__rendererId = propertyNamespace;
    component[rendererName] = renderer;
};

_window.Vaadin.unsetReactRenderer = (component: Component, rendererName: string, rendererId: string): void => {
    const renderer: Renderer | undefined = component[rendererName];
    if(renderer?.__rendererId !== rendererId) {
        return;
    }

    // Clean up React root before removing renderer
    const elements: NodeListOf<Element> = component.querySelectorAll(`[data-renderer-id="${rendererId}"]`);
    for(const element of elements) {
        if(!('__reactRoot' in element)) {
            continue;
        }
        (element.__reactRoot as any).unmount();
        delete element.__reactRoot;
    }

    component[rendererName] = undefined;
};
