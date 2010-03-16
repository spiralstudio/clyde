//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.tudey.server.logic;

import java.lang.reflect.Field;
import java.util.ArrayList;

import com.google.common.collect.Lists;

import com.samskivert.util.ListUtil;

import com.threerings.math.FloatMath;
import com.threerings.math.Vector2f;

import com.threerings.tudey.data.actor.Actor;
import com.threerings.tudey.config.ConditionConfig;
import com.threerings.tudey.server.TudeySceneManager;
import com.threerings.tudey.shape.Shape;

import static com.threerings.tudey.Log.*;

/**
 * Handles the evaluation of conditions.
 */
public abstract class ConditionLogic extends Logic
{
    /**
     * Evaluates the tagged condition.
     */
    public static class Tagged extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            ConditionConfig.Tagged config = (ConditionConfig.Tagged)_config;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (ListUtil.contains(_targets.get(ii).getTags(), config.tag) != config.all) {
                        return !config.all;
                    }
                }
                return config.all;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.Tagged)_config).target, _source);
        }

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the instance of condition.
     */
    public static class InstanceOf extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            boolean all = ((ConditionConfig.InstanceOf)_config).all;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    if (_logicClass.isInstance(_targets.get(ii)) != all) {
                        return !all;
                    }
                }
                return all;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.InstanceOf config = (ConditionConfig.InstanceOf)_config;
            try {
                _logicClass = Class.forName(config.logicClass);
            } catch (ClassNotFoundException e) {
                log.warning("Missing logic class for InstanceOf condition.", e);
                _logicClass = Logic.class;
            }
            _target = createTarget(config.target, _source);
        }

        /** The test class. */
        protected Class<?> _logicClass;

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the intersect condition logic.
     */
    public static class Intersecting extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            boolean all = ((ConditionConfig.Intersecting)_config).allFirst;
            _first.resolve(activator, _firsts);
            _second.resolve(activator, _seconds);
            try {
                for (int ii = 0, nn = _firsts.size(); ii < nn; ii++) {
                    if (intersectsSecond(_firsts.get(ii)) != all) {
                        return !all;
                    }
                }
                return all;

            } finally {
                _firsts.clear();
                _seconds.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.Intersecting config = (ConditionConfig.Intersecting)_config;
            _first = createRegion(config.first, _source);
            _second = createRegion(config.second, _source);
        }

        /**
         * Determines whether the specified shape from the first target satisfies the intersection
         * condition.
         */
        protected boolean intersectsSecond (Shape shape)
        {
            boolean all = ((ConditionConfig.Intersecting)_config).allSecond;
            for (int ii = 0, nn = _seconds.size(); ii < nn; ii++) {
                if (shape.intersects(_seconds.get(ii)) != all) {
                    return !all;
                }
            }
            return all;
        }

        /** The regions to check. */
        protected RegionLogic _first, _second;

        /** Holds shapes during evaluation. */
        protected ArrayList<Shape> _firsts = Lists.newArrayList(), _seconds = Lists.newArrayList();
    }

    /**
     * Evaluates the distance within condition logic.
     */
    public static class DistanceWithin extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            boolean all = ((ConditionConfig.DistanceWithin)_config).allFirst;
            _first.resolve(activator, _firsts);
            _second.resolve(activator, _seconds);
            try {
                for (int ii = 0, nn = _firsts.size(); ii < nn; ii++) {
                    if (withinSecond(_firsts.get(ii).getTranslation()) != all) {
                        return !all;
                    }
                }
                return all;

            } finally {
                _firsts.clear();
                _seconds.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            _first = createTarget(config.first, _source);
            _second = createTarget(config.second, _source);
        }

        /**
         * Determines whether the specified shape from the first target satisfies the distance
         * condition.
         */
        protected boolean withinSecond (Vector2f t1)
        {
            ConditionConfig.DistanceWithin config = (ConditionConfig.DistanceWithin)_config;
            for (int ii = 0, nn = _seconds.size(); ii < nn; ii++) {
                Vector2f t2 = _seconds.get(ii).getTranslation();
                if (FloatMath.isWithin(t1.distance(t2), config.minimum, config.maximum) !=
                        config.allSecond) {
                    return !config.allSecond;
                }
            }
            return config.allSecond;
        }

        /** The targets to check. */
        protected TargetLogic _first, _second;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _firsts = Lists.newArrayList(), _seconds = Lists.newArrayList();
    }

    /**
     * Evaluates the random condition.
     */
    public static class Random extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            return FloatMath.random() < ((ConditionConfig.Random)_config).probability;
        }
    }

    /**
     * Evaluates the limit condition.
     */
    public static class Limit extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            if (_limit > 0) {
                _limit--;
                return true;
            }
            return false;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _limit = ((ConditionConfig.Limit)_config).limit;
        }

        /** The remaining limit. */
        protected int _limit;
    }

    /**
     * Evaluates the all condition.
     */
    public static class All extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (!condition.isSatisfied(activator)) {
                    return false;
                }
            }
            return true;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<ConditionLogic> list = Lists.newArrayList();
            for (ConditionConfig config : ((ConditionConfig.All)_config).conditions) {
                ConditionLogic condition = createCondition(config, _source);
                if (condition != null) {
                    list.add(condition);
                }
            }
            _conditions = list.toArray(new ConditionLogic[list.size()]);
        }

        /** The component conditions. */
        protected ConditionLogic[] _conditions;
    }

    /**
     * Evaluates the any condition.
     */
    public static class Any extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            for (ConditionLogic condition : _conditions) {
                if (condition.isSatisfied(activator)) {
                    return true;
                }
            }
            return false;
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            ArrayList<ConditionLogic> list = Lists.newArrayList();
            for (ConditionConfig config : ((ConditionConfig.Any)_config).conditions) {
                ConditionLogic condition = createCondition(config, _source);
                if (condition != null) {
                    list.add(condition);
                }
            }
            _conditions = list.toArray(new ConditionLogic[list.size()]);
        }

        /** The component conditions. */
        protected ConditionLogic[] _conditions;
    }

    /**
     * Evaluates the flag set condition.
     */
    public static class FlagSet extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            ConditionConfig.FlagSet config = (ConditionConfig.FlagSet)_config;
            _target.resolve(activator, _targets);
            try {
                for (int ii = 0, nn = _targets.size(); ii < nn; ii++) {
                    Logic logic = _targets.get(ii);
                    if (logic instanceof ActorLogic) {
                        Actor actor = ((ActorLogic)logic).getActor();
                        try {
                            Field flag = actor.getClass().getField(config.flagName);
                            if (actor.isSet(flag.getInt(actor))) {
                                return config.set;
                            }
                        } catch (NoSuchFieldException e) {
                            log.warning("Flag field not found in class for Flag Set Condition.", e);
                        } catch (IllegalAccessException e) {
                            log.warning("Cannot access flag field for Flag Set Condition.", e);
                        }
                    }
                }
                return !config.set;

            } finally {
                _targets.clear();
            }
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _target = createTarget(((ConditionConfig.FlagSet)_config).target, _source);
        }

        /** The target to check. */
        protected TargetLogic _target;

        /** Holds targets during evaluation. */
        protected ArrayList<Logic> _targets = Lists.newArrayList();
    }

    /**
     * Evaluates the cooldown condition.
     */
    public static class Cooldown extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            int timestamp = _scenemgr.getTimestamp();
            if (_nextTimestamp < 0 || timestamp > _nextTimestamp) {
                _nextTimestamp = timestamp + ((ConditionConfig.Cooldown)_config).time;
                return true;
            }
            return false;
        }

        /** The next timestamp before we'll be satisfied. */
        protected int _nextTimestamp = -1;
    }

    /**
     * Evaluates the not condition.
     */
    public static class Not extends ConditionLogic
    {
        @Override // documentation inherited
        public boolean isSatisfied (Logic activator)
        {
            return !_condition.isSatisfied(activator);
        }

        @Override // documentation inherited
        protected void didInit ()
        {
            _condition = createCondition(((ConditionConfig.Not)_config).condition, _source);
        }

        /** The component condition. */
        protected ConditionLogic _condition;
    }

    /**
     * Evaluates the always condition.
     */
    public static class Always extends ConditionLogic
    {
        @Override // documentaiton inherited
        public boolean isSatisfied (Logic activator)
        {
            return true;
        }
    }

    /**
     * Initializes the logic.
     */
    public void init (TudeySceneManager scenemgr, ConditionConfig config, Logic source)
    {
        super.init(scenemgr);
        _config = config;
        _source = source;

        // give subclasses a chance to initialize
        didInit();
    }

    /**
     * Determines whether the condition is satisfied.
     *
     * @param activator the entity that triggered the action.
     */
    public abstract boolean isSatisfied (Logic activator);

    @Override // documentation inherited
    public boolean isActive ()
    {
        return _source.isActive();
    }

    @Override // documentation inherited
    public Vector2f getTranslation ()
    {
        return _source.getTranslation();
    }

    @Override // documentation inherited
    public float getRotation ()
    {
        return _source.getRotation();
    }

    /**
     * Override to perform custom initialization.
     */
    protected void didInit ()
    {
        // nothing by default
    }

    /** The condition configuration. */
    protected ConditionConfig _config;

    /** The action source. */
    protected Logic _source;
}
