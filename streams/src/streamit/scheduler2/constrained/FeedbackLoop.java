/*
 * Copyright 2003 by the Massachusetts Institute of Technology.
 *
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting
 * documentation, and that the name of M.I.T. not be used in
 * advertising or publicity pertaining to distribution of the
 * software without specific, written prior permission.
 * M.I.T. makes no representations about the suitability of
 * this software for any purpose.  It is provided "as is"
 * without express or implied warranty.
 */

package streamit.scheduler2.constrained;

import streamit.scheduler2.iriter./*persistent.*/
    FeedbackLoopIter;
import streamit.scheduler2.iriter./*persistent.*/
    Iterator;

import streamit.scheduler2.hierarchical.PhasingSchedule;


/**
 * streamit.scheduler2.constrained.Pipeline is the pipeline constrained 
 * scheduler. It assumes that all streams in the program use the constrained
 * scheduler
 */

public class FeedbackLoop
    extends streamit.scheduler2.hierarchical.FeedbackLoop
    implements StreamInterface
{
    final private LatencyGraph latencyGraph;

    LatencyNode latencySplitter, latencyJoiner;

    public FeedbackLoop(
                        FeedbackLoopIter iterator,
                        Iterator parent,
                        streamit.scheduler2.constrained.StreamFactory factory)
    {
        super(iterator, factory);

        latencyGraph = factory.getLatencyGraph();

        if (parent == null)
            {
                latencyGraph.registerParent(this, null);
                initiateConstrained();
            }
    }

    public void initiateConstrained()
    {
        latencySplitter = latencyGraph.addSplitter(this);
        latencyJoiner = latencyGraph.addJoiner(this);

        // register body and loop
        {
            StreamInterface body = getConstrainedBody();
            latencyGraph.registerParent(body, this);
            body.initiateConstrained();

            StreamInterface loop = getConstrainedLoop();
            latencyGraph.registerParent(loop, this);
            loop.initiateConstrained();
        }

        // add body and loop to the latency graph
        {
            // first the body
            {
                StreamInterface body = getConstrainedBody();

                LatencyNode topBodyNode = body.getTopLatencyNode();
                LatencyNode bottomBodyNode = body.getBottomLatencyNode();

                //create the appropriate edges
                LatencyEdge topBodyEdge =
                    new LatencyEdge(latencyJoiner, 0, topBodyNode, 0, 0);
                latencyJoiner.addDependency(topBodyEdge);
                topBodyNode.addDependency(topBodyEdge);

                LatencyEdge bottomBodyEdge =
                    new LatencyEdge(
                                    bottomBodyNode,
                                    0,
                                    latencySplitter,
                                    0,
                                    0);
                latencySplitter.addDependency(bottomBodyEdge);
                bottomBodyNode.addDependency(bottomBodyEdge);
            }
            // and now the loop
            {
                StreamInterface loop = getConstrainedLoop();

                LatencyNode topLoopNode = loop.getTopLatencyNode();
                LatencyNode bottomLoopNode = loop.getBottomLatencyNode();

                //create the appropriate edges
                LatencyEdge topLoopEdge =
                    new LatencyEdge(latencySplitter, 1, topLoopNode, 0, 0);
                latencySplitter.addDependency(topLoopEdge);
                topLoopNode.addDependency(topLoopEdge);

                LatencyEdge bottomLoopEdge =
                    new LatencyEdge(
                                    bottomLoopNode,
                                    0,
                                    latencyJoiner,
                                    1,
                                    feedbackLoop.getDelaySize());
                latencyJoiner.addDependency(bottomLoopEdge);
                bottomLoopNode.addDependency(bottomLoopEdge);
            }
        }
    }

    public StreamInterface getTopConstrainedStream()
    {
        return this;
    }

    public StreamInterface getBottomConstrainedStream()
    {
        return this;
    }

    protected StreamInterface getConstrainedLoop()
    {
        if (!(getLoop() instanceof StreamInterface))
            {
                ERROR("This feedbackloop contains a loop that is not CONSTRAINED");
            }

        return (StreamInterface)getLoop();
    }

    protected StreamInterface getConstrainedBody()
    {
        if (!(getBody() instanceof StreamInterface))
            {
                ERROR("This feedbackloop contains a body that is not CONSTRAINED");
            }

        return (StreamInterface)getBody();
    }

    public LatencyNode getBottomLatencyNode()
    {
        return latencySplitter;
    }

    public LatencyNode getTopLatencyNode()
    {
        return latencyJoiner;
    }

    public void computeSchedule()
    {
        ERROR("Not implemented yet.");

    }
    


    public void registerConstraint(P2PPortal portal)
    {
        ERROR ("not implemented");
    }
    
    public void createSteadyStateRestrictions(int streamNumExecs)
    {
        ERROR ("not implemented");
    }
    
    public void initRestrictionsCompleted(P2PPortal portal)
    {
        ERROR ("not implemented");
    }
    
    public void initializeRestrictions(Restrictions _restrictions)
    {
        ERROR ("not implemented");
    }
    
    public boolean isDoneInitializing ()
    {
        ERROR ("not implemented");
        return false;
    }

    public PhasingSchedule getNextPhase(
                                        Restrictions restrs,
                                        int nDataAvailable)
    {
        ERROR("not implemented");
        return null;
    }
    
    public void registerNewlyBlockedSteadyRestriction(Restriction restriction)
    {
        ERROR("not implemented");
    }
    
    public boolean isDoneSteadyState ()
    {
        ERROR("not implemented");
        return false;
    }

    public void doneSteadyState (LatencyNode node)
    {
        ERROR("not implemented");
    }
}
