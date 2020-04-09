package org.monarchinitiative.lirical.cmd;

import org.monarchinitiative.lirical.exception.LiricalException;

public abstract class LiricalCommand {
    public abstract void run() throws LiricalException;
}
