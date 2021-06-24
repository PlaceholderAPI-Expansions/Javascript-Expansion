package com.extendedclip.papi.expansion.javascript.evaluator;

import com.eclipsesource.v8.*;
import io.alicorn.v8.V8JavaAdapter;

import java.util.Map;

public final class J2V8ScriptEvaluator implements ScriptEvaluator {
    private final Map<String, Object> bindings;

    public J2V8ScriptEvaluator(final Map<String, Object> bindings) {
        this.bindings = bindings;
    }

    @Override
    public Object execute(final Map<String, Object> additionalBindings, final String script) throws EvaluatorException {
        try (final V8 v8 = V8.createV8Runtime();
            final ClosableMemoryManager closableMemoryManager = new ClosableMemoryManager(v8)) {
            // Memory manager to handle bound reference management
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                bind(v8, entry.getKey(), entry.getValue());
            }
            for (Map.Entry<String, Object> entry : additionalBindings.entrySet()) {
                bind(v8, entry.getKey(), entry.getValue());
            }
            return v8.executeScript(script);
        } catch (final Exception exception) {
            throw new EvaluatorException("Failed to evaluate requested script.", exception);
        }
    }

    private void bind(final V8 v8, final String key, final Object value) {
        if (value.getClass().isArray()) {
            final Object[] rawArray = (Object[]) value;
            final V8Array jsArray = new V8Array(v8);
            for (Object o : rawArray) {
                jsArray.push(o);
            }
            v8.add(key, jsArray);
            // Drop native handle
            jsArray.close();
            return;
        }
        V8JavaAdapter.injectObject(key, value, v8);
    }
}