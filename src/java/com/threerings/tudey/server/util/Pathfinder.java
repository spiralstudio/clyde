//
// $Id$

package com.threerings.tudey.server.util;

import java.awt.Point;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import com.samskivert.util.StringUtil;

import com.threerings.media.util.AStarPathUtil;

import com.threerings.config.ConfigManager;
import com.threerings.math.FloatMath;
import com.threerings.math.Rect;
import com.threerings.math.Transform2D;
import com.threerings.math.Vector2f;

import com.threerings.opengl.gui.util.Rectangle;

import com.threerings.tudey.config.TileConfig;
import com.threerings.tudey.data.TudeySceneModel;
import com.threerings.tudey.data.TudeySceneModel.Entry;
import com.threerings.tudey.data.TudeySceneModel.TileEntry;
import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.server.logic.ActorLogic;
import com.threerings.tudey.server.logic.Logic;
import com.threerings.tudey.shape.Polygon;
import com.threerings.tudey.shape.Shape;
import com.threerings.tudey.shape.ShapeElement;
import com.threerings.tudey.space.Space;
import com.threerings.tudey.space.SpaceElement;
import com.threerings.tudey.util.CoordIntMap;

/**
 * A helper class for pathfinding.  Currently the pathfinding strategy is to divide the world up
 * into unit cells and track the collision flags of all scene entries and actors whose shapes
 * intersect those cells.  An alternate method that may be worth exploring would be to have the
 * traversal predicate perform a full intersection query (it seems likely that this would be more
 * expensive than maintaining the collision map for all actors, but it's not entirely clear).
 */
public class Pathfinder
    implements TudeySceneModel.Observer, TudeySceneManager.ActorObserver, Logic.ShapeObserver
{
    /**
     * Creates a new pathfinder.
     */
    public Pathfinder (TudeySceneManager scenemgr)
    {
        _scenemgr = scenemgr;

        // initialize the entry flags and register as an observer
        TudeySceneModel model = (TudeySceneModel)_scenemgr.getScene().getSceneModel();
        _entryFlags.putAll(model.getCollisionFlags());
        _combinedFlags.putAll(_entryFlags);
        for (SpaceElement element : model.getElements().values()) {
            addFlags((Entry)element.getUserObject());
        }
        model.addObserver(this);

        // listen for actor updates
        _scenemgr.addActorObserver(this);
    }

    /**
     * Shuts down the pathfinder.
     */
    public void shutdown ()
    {
        ((TudeySceneModel)_scenemgr.getScene().getSceneModel()).removeObserver(this);
        _scenemgr.removeActorObserver(this);
    }

    /**
     * Computes a path for the specified actor from its current location, considering only the
     * scene entries (not the actors).
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @param shortcut if true, use swept shapes to find path shortcuts.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getEntryPath (
        ActorLogic actor, float longest, float bx, float by, boolean partial, boolean shortcut)
    {
        Vector2f translation = actor.getTranslation();
        return getEntryPath(
            actor, longest, translation.x, translation.y, bx, by, partial, shortcut);
    }

    /**
     * Computes a path for the specified actor, considering only the scene entries (not the
     * actors).
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @param shortcut if true, use swept shapes to find path shortcuts.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getEntryPath (
        ActorLogic actor, float longest, float ax, float ay,
        float bx, float by, boolean partial, boolean shortcut)
    {
        return getPath(_entryFlags, actor, longest, ax, ay, bx, by, partial, shortcut);
    }

    /**
     * Computes a path for the specified actor from its current location.
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @param shortcut if true, use swept shapes to find path shortcuts.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getPath (
        ActorLogic actor, float longest, float bx, float by, boolean partial, boolean shortcut)
    {
        Vector2f translation = actor.getTranslation();
        return getPath(actor, longest, translation.x, translation.y, bx, by, partial, shortcut);
    }

    /**
     * Computes a path for the specified actor.
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @param shortcut if true, use swept shapes to find path shortcuts.
     * @return the computed path, or null if unreachable.
     */
    public Vector2f[] getPath (
        ActorLogic actor, float longest, float ax, float ay,
        float bx, float by, boolean partial, boolean shortcut)
    {
        return getPath(_combinedFlags, actor, longest, ax, ay, bx, by, partial, shortcut);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryAdded (Entry entry)
    {
        addFlags(entry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryUpdated (Entry oentry, Entry nentry)
    {
        removeFlags(oentry);
        addFlags(nentry);
    }

    // documentation inherited from interface TudeySceneModel.Observer
    public void entryRemoved (Entry oentry)
    {
        removeFlags(oentry);
    }

    // documentation inherited from interface TudeySceneManager.ActorObserver
    public void actorAdded (ActorLogic logic)
    {
        addFlags(logic);
        logic.addShapeObserver(this);
    }

    // documentation inherited from interface TudeySceneManager.ActorObserver
    public void actorRemoved (ActorLogic logic)
    {
        removeFlags(logic);
        logic.removeShapeObserver(this);
    }

    // documentation inherited from Logic.ShapeObserver
    public void shapeWillChange (Logic logic)
    {
        removeFlags((ActorLogic)logic);
    }

    // documentation inherited from Logic.ShapeObserver
    public void shapeDidChange (Logic logic)
    {
        addFlags((ActorLogic)logic);
    }

    /**
     * Computes a path for the specified actor.
     *
     * @param longest the maximum path length.
     * @param partial if true, return a partial path even if the destination is unreachable.
     * @param shortcut if true, use swept shapes to compute shortcuts in the path.
     * @return the computed path, or null if unreachable.
     */
    protected Vector2f[] getPath (
        final CoordIntMap flags, ActorLogic logic, float longest, float ax, float ay,
        float bx, float by, boolean partial, boolean shortcut)
    {
        // first things first: are we there already?
        Vector2f start = new Vector2f(ax, ay);
        if (ax == bx && ay == by) {
            return new Vector2f[] { start };
        }

        // can we simply slide on over?
        Vector2f end = new Vector2f(bx, by);
        if (!sweptShapeCollides(flags, logic, start, end)) {
            return new Vector2f[] { start, end };
        }

        // determine the actor's extents
        Rect bounds = logic.getShape().getBounds();
        int width = Math.max(1, (int)Math.ceil(bounds.getWidth()));
        int height = Math.max(1, (int)Math.ceil(bounds.getHeight()));

        // create the traversal predicate
        AStarPathUtil.TraversalPred pred;
        final Actor actor = logic.getActor();
        if (width == 1 && height == 1) {
            // simpler predicate for the common case of 1x1 actors
            pred = new AStarPathUtil.TraversalPred() {
                public boolean canTraverse (Object traverser, int x, int y) {
                    return !actor.canCollide(flags.get(x, y));
                }
            };
        } else {
            final int left = width / 2, right = (width - 1) / 2;
            final int bottom = height / 2, top = (height - 1) / 2;
            pred = new AStarPathUtil.TraversalPred() {
                public boolean canTraverse (Object traverser, int x, int y) {
                    for (int yy = y - bottom, yymax = y + top; yy <= yymax; yy++) {
                        for (int xx = x - left, xxmax = x + right; xx <= xxmax; xx++) {
                            if (actor.canCollide(flags.get(xx, yy))) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
            };
        }

        // compute the offsets for converting to/from integer coordinates
        float xoff = (width % 2) * 0.5f;
        float yoff = (height % 2) * 0.5f;

        // if the actor is in the space and can collide with its own flags,
        // remove them before we compute the path
        boolean remove = (!logic.isRemoved() && flags == _combinedFlags &&
            actor.canCollide(actor.getCollisionFlags()));
        if (remove) {
            removeFlags(logic);
        }

        // compute the path
        List<Point> path = AStarPathUtil.getPath(
            pred, actor, (int)longest, Math.round(ax - xoff), Math.round(ay - yoff),
            Math.round(bx - xoff), Math.round(by - yoff), partial);

        // add the flags back if we removed them
        if (remove) {
            addFlags(logic);
        }

        // convert to fractional coordinates
        if (path == null) {
            return null;
        }
        Vector2f[] waypoints = new Vector2f[path.size()];
        for (int ii = 0; ii < waypoints.length; ii++) {
            Point pt = path.get(ii);
            waypoints[ii] = new Vector2f(pt.x + xoff, pt.y + yoff);
        }

        // process for shortcuts if requested
        if (!shortcut) {
            return waypoints;
        }
        Vector2f current = start;
        for (int ii = 0; ii < waypoints.length; ) {
            for (int jj = waypoints.length - 1; jj >= ii; jj--) {
                Vector2f waypoint = waypoints[jj];
                if (jj == ii || !sweptShapeCollides(flags, logic, current, waypoint)) {
                    _waypoints.add(current = waypoint);
                    ii = jj + 1;
                    break;
                }
            }
        }
        waypoints = _waypoints.toArray(new Vector2f[_waypoints.size()]);
        _waypoints.clear();
        return waypoints;
    }

    /**
     * Determines whether the swept shape of the specified actor collides with anything.
     */
    protected boolean sweptShapeCollides (
        CoordIntMap flags, ActorLogic logic, Vector2f start, Vector2f end)
    {
        _worldShape = logic.getShapeElement().getLocalShape().transform(
            _transform.set(start, logic.getRotation()), _worldShape);
        _sweptShape = _worldShape.sweep(end.subtract(start, _translation), _sweptShape);
        if (flags == _entryFlags) {
            return ((TudeySceneModel)_scenemgr.getScene().getSceneModel()).collides(
                logic.getActor(), _sweptShape);
        } else {
            return _scenemgr.collides(logic, _sweptShape);
        }
    }

    /**
     * Adds the specified entry's flags to the flag maps.
     */
    protected void addFlags (Entry entry)
    {
        if (!(entry instanceof TileEntry)) {
            ConfigManager cfgmgr = _scenemgr.getConfigManager();
            Shape shape = entry.createShape(cfgmgr);
            if (shape != null) {
                addFlags(shape, entry.getCollisionFlags(cfgmgr), true);
            }
            return;
        }
        TileEntry tentry = (TileEntry)entry;
        TileConfig.Original config = tentry.getConfig(_scenemgr.getConfigManager());
        tentry.getRegion(config, _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                int flags = tentry.getCollisionFlags(config, xx, yy);
                if (flags != 0) {
                    _entryFlags.setBits(xx, yy, flags);
                    _combinedFlags.setBits(xx, yy, flags);
                }
            }
        }
    }

    /**
     * Removes the flags for the specified entry.
     */
    protected void removeFlags (Entry entry)
    {
        if (!(entry instanceof TileEntry)) {
            ConfigManager cfgmgr = _scenemgr.getConfigManager();
            Shape shape = entry.createShape(cfgmgr);
            if (shape != null) {
                removeFlags(shape, entry.getCollisionFlags(cfgmgr), true, null);
            }
            return;
        }
        TileEntry tentry = (TileEntry)entry;
        TileConfig.Original config = tentry.getConfig(_scenemgr.getConfigManager());
        tentry.getRegion(config, _region);
        for (int yy = _region.y, yymax = yy + _region.height; yy < yymax; yy++) {
            for (int xx = _region.x, xxmax = xx + _region.width; xx < xxmax; xx++) {
                int flags = tentry.getCollisionFlags(config, xx, yy);
                if (flags != 0) {
                    updateQuad(xx, yy);
                    updateFlags(xx, yy, true, null);
                }
            }
        }
    }

    /**
     * Adds the flags for the specified actor.
     */
    protected void addFlags (ActorLogic logic)
    {
        addFlags(logic.getShape(), logic.getActor().getCollisionFlags(), false);
    }

    /**
     * Removes the flags for the specified actor.
     */
    protected void removeFlags (ActorLogic logic)
    {
        removeFlags(
            logic.getShape(), logic.getActor().getCollisionFlags(),
            false, logic.getShapeElement());
    }

    /**
     * Adds the specified flags to the flag map(s).
     */
    protected void addFlags (Shape shape, int flags, boolean entry)
    {
        if (flags == 0) {
            return; // nothing to do
        }
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                updateQuad(xx, yy);
                if (shape.intersects(_quad)) {
                    if (entry) {
                        _entryFlags.setBits(xx, yy, flags);
                    }
                    _combinedFlags.setBits(xx, yy, flags);
                }
            }
        }
    }

    /**
     * Removes the flags for the specified shape.
     */
    protected void removeFlags (Shape shape, int flags, boolean entry, SpaceElement skip)
    {
        if (flags == 0) {
            return; // nothing to do
        }
        Rect bounds = shape.getBounds();
        Vector2f min = bounds.getMinimumExtent(), max = bounds.getMaximumExtent();
        int minx = (int)FloatMath.floor(min.x);
        int maxx = (int)FloatMath.floor(max.x);
        int miny = (int)FloatMath.floor(min.y);
        int maxy = (int)FloatMath.floor(max.y);
        for (int yy = miny; yy <= maxy; yy++) {
            for (int xx = minx; xx <= maxx; xx++) {
                updateQuad(xx, yy);
                if (shape.intersects(_quad)) {
                    updateFlags(xx, yy, entry, skip);
                }
            }
        }
    }

    /**
     * Updates the coordinates of the quad to encompass the specified grid cell.
     */
    protected void updateQuad (int x, int y)
    {
        float lx = x, ly = y, ux = lx + 1f, uy = ly + 1f;
        _quad.getVertex(0).set(lx, ly);
        _quad.getVertex(1).set(ux, ly);
        _quad.getVertex(2).set(ux, uy);
        _quad.getVertex(3).set(lx, uy);
        _quad.getBounds().getMinimumExtent().set(lx, ly);
        _quad.getBounds().getMaximumExtent().set(ux, uy);
    }

    /**
     * Updates the flags at the specified location ({@link #_quad} should be set to the cell
     * boundaries).
     *
     * @param skip an element to skip, or null for none.
     */
    protected void updateFlags (int x, int y, boolean entry, SpaceElement skip)
    {
        // if we're updating an entry, recompute its flags; otherwise, retrieve from map
        int flags;
        if (entry) {
            TudeySceneModel model = (TudeySceneModel)_scenemgr.getScene().getSceneModel();
            flags = model.getCollisionFlags().get(x, y);
            model.getSpace().getIntersecting(_quad, _elements);
            ConfigManager cfgmgr = _scenemgr.getConfigManager();
            for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
                SpaceElement element = _elements.get(ii);
                if (element != skip) {
                    flags |= ((Entry)element.getUserObject()).getCollisionFlags(cfgmgr);
                }
            }
            _elements.clear();
            _entryFlags.put(x, y, flags);
        } else {
            flags = _entryFlags.get(x, y);
        }

        // add the flags for the actors
        _scenemgr.getActorSpace().getIntersecting(_quad, _elements);
        for (int ii = 0, nn = _elements.size(); ii < nn; ii++) {
            SpaceElement element = _elements.get(ii);
            if (element != skip) {
                flags |= ((ActorLogic)element.getUserObject()).getActor().getCollisionFlags();
            }
        }
        _elements.clear();

        // store the combined flags
        _combinedFlags.put(x, y, flags);
    }

    /** The owning scene manager. */
    protected TudeySceneManager _scenemgr;

    /** The collision flags corresponding to the scene entries. */
    protected CoordIntMap _entryFlags = new CoordIntMap(3, 0);

    /** The collision flags corresponding to the scene entries and the actors. */
    protected CoordIntMap _combinedFlags = new CoordIntMap(3, 0);

    /** Used to store tile shapes for intersecting testing. */
    protected Polygon _quad = new Polygon(4);

    /** Holds elements during intersection testing. */
    protected List<SpaceElement> _elements = Lists.newArrayList();

    /** Holds waypoints during shortcut processing. */
    protected List<Vector2f> _waypoints = Lists.newArrayList();

    /** Region object to reuse. */
    protected Rectangle _region = new Rectangle();

    /** Transform to reuse. */
    protected Transform2D _transform = new Transform2D();

    /** World shape to reuse. */
    protected Shape _worldShape;

    /** Translation vector to reuse. */
    protected Vector2f _translation = new Vector2f();

    /** Swept shape to reuse. */
    protected Shape _sweptShape;
}
