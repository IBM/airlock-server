package com.ibm.airlock.engine;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

/**
 * Created by DenisV on 8/31/16.
 */
public class SafeContextFactory extends ContextFactory {

        // Custom Context to store execution time.
        private static class SafeContext extends Context
        {
            long startTime;
        }

        static {
            // Initialize GlobalFactory with custom factory
            ContextFactory.initGlobal(new SafeContextFactory());
        }


        public Context makeContext()
        {
            SafeContext cx = new SafeContext();
            // Use pure interpreter mode to allow for
            // observeInstructionCount(Context, int) to work
            cx.setOptimizationLevel(-1);
            // Make Rhino runtime to call observeInstructionCount
            // each 10000 bytecode instructions
            cx.setInstructionObserverThreshold(10000);
            return cx;
        }

        // Override hasFeature(Context, int)
        public boolean hasFeature(Context cx, int featureIndex)
        {
            // Turn on maximum compatibility with MSIE scripts
            switch (featureIndex) {
                case Context.FEATURE_NON_ECMA_GET_YEAR:
                    return true;

                case Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
                    return true;

                case Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER:
                    return true;

                case Context.FEATURE_PARENT_PROTO_PROPERTIES:
                    return false;
            }
            return super.hasFeature(cx, featureIndex);
        }

        static final int MAX_SECONDS = 10;
        protected void observeInstructionCount(Context cx, int instructionCount)
        {
            SafeContext mcx = (SafeContext)cx;
            long currentTime = System.currentTimeMillis();
            if (currentTime - mcx.startTime > MAX_SECONDS*1000) {
                // More then 10 seconds from Context creation time:
                // it is time to stop the script.
                // Throw Error instance to ensure that script will never
                // get control back through catch or finally.
                throw new ScriptExecutionTimeoutException();
            }
        }


        protected Object doTopCall(Callable callable,
                                   Context cx, Scriptable scope,
                                   Scriptable thisObj, Object[] args)
        {
            SafeContext mcx = (SafeContext)cx;
            mcx.startTime = System.currentTimeMillis();
            return super.doTopCall(callable, cx, scope, thisObj, args);
        }
}
