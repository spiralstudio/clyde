//
// $Id$

package com.threerings.expr;

/**
 * Provides a means to resolve symbols in a dynamic, hierarchical fashion.  Symbols can be mapped
 * to {@link Function}s, {@link Variable}s, or arbitrary objects (often mutable ones, so that
 * values can change after resolution).
 */
public interface Scope
{
    /**
     * Returns the name of this scope for purposes of qualification.  Can return <code>null</code>
     * if qualified symbols cannot specifically address this scope.
     */
    public String getScopeName ();
    
    /**
     * Returns a reference to the parent scope, or <code>null</code> if this is the top level.
     */
    public Scope getParentScope ();
        
    /**
     * Looks up a symbol in this scope.
     *
     * @return the mapping for the requested symbol, or <code>null</code> if not found.
     */
    public <T> T get (String name, Class<T> clazz);
    
    /**
     * Adds a listener for changes in scope.  The listener will be notified when symbols are
     * added or removed and whenever the scope hierarchy changes.
     */
    public void addListener (ScopeUpdateListener listener);
    
    /**
     * Removes a listener for changes in scope.
     */
    public void removeListener (ScopeUpdateListener listener);
}
