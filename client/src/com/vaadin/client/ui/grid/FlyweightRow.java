/*
 * Copyright 2000-2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.client.ui.grid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Element;

/**
 * An internal implementation of the {@link Row} interface.
 * <p>
 * There is only one instance per Escalator. This is designed to be re-used when
 * rendering rows.
 * 
 * @since 7.2
 * @author Vaadin Ltd
 * @see Escalator.AbstractRowContainer#refreshRow(Node, int)
 */
class FlyweightRow implements Row {

    static class CellIterator implements Iterator<Cell> {
        /** A defensive copy of the cells in the current row. */
        private final ArrayList<FlyweightCell> cells;
        private int cursor = 0;
        private int skipNext = 0;

        public CellIterator(final Collection<FlyweightCell> cells) {
            this.cells = new ArrayList<FlyweightCell>(cells);
        }

        @Override
        public boolean hasNext() {
            return cursor + skipNext < cells.size();
        }

        @Override
        public FlyweightCell next() {
            // if we needed to skip some cells since the last invocation.
            for (int i = 0; i < skipNext; i++) {
                cells.remove(cursor);
            }
            skipNext = 0;

            final FlyweightCell cell = cells.get(cursor++);
            cell.setup(this);
            return cell;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove cells via iterator");
        }

        /**
         * Sets the number of cells to skip when {@link #next()} is called the
         * next time. Cell hiding is also handled eagerly in this method.
         * 
         * @param colspan
         *            the number of cells to skip on next invocation of
         *            {@link #next()}
         */
        public void setSkipNext(final int colspan) {
            assert colspan > 0 : "Number of cells didn't make sense: "
                    + colspan;
            skipNext = colspan;
        }

        /**
         * Gets the next <code>n</code> cells in the iterator, ignoring any
         * possibly spanned cells.
         * 
         * @param n
         *            the number of next cells to retrieve
         * @return A list of next <code>n</code> cells, or less if there aren't
         *         enough cells to retrieve
         */
        public List<FlyweightCell> rawPeekNext(final int n) {
            final int from = Math.min(cursor, cells.size());
            final int to = Math.min(cursor + n, cells.size());
            return cells.subList(from, to);
        }
    }

    private static final int BLANK = Integer.MIN_VALUE;

    private int row;
    private Element element;
    private int[] columnWidths = null;
    private final Escalator escalator;
    private final List<FlyweightCell> cells = new ArrayList<FlyweightCell>();

    public FlyweightRow(final Escalator escalator) {
        this.escalator = escalator;
    }

    @Override
    public Escalator getEscalator() {
        return escalator;
    }

    void setup(final Element e, final int row, int[] columnWidths) {
        element = e;
        this.row = row;
        this.columnWidths = columnWidths;
    }

    /**
     * Tear down the state of the Row.
     * <p>
     * This is an internal check method, to prevent retrieving uninitialized
     * data by calling {@link #getRow()}, {@link #getElement()} or
     * {@link #getCells()} at an improper time.
     * <p>
     * This should only be used with asserts ("
     * <code>assert flyweightRow.teardown()</code> ") so that the code is never
     * run when asserts aren't enabled.
     * 
     * @return always <code>true</code>
     */
    boolean teardown() {
        element = null;
        row = BLANK;
        columnWidths = null;
        for (final FlyweightCell cell : cells) {
            assert cell.teardown();
        }
        return true;
    }

    @Override
    public int getRow() {
        assertSetup();
        return row;
    }

    @Override
    public Element getElement() {
        assertSetup();
        return element;
    }

    void addCells(final int index, final int numberOfColumns) {
        for (int i = 0; i < numberOfColumns; i++) {
            final int col = index + i;
            cells.add(col, new FlyweightCell(this, col, escalator));
        }
        updateRestOfCells(index + numberOfColumns);
    }

    void removeCells(final int index, final int numberOfColumns) {
        for (int i = 0; i < numberOfColumns; i++) {
            cells.remove(index);
        }
        updateRestOfCells(index);
    }

    private void updateRestOfCells(final int startPos) {
        // update the column number for the cells to the right
        for (int col = startPos; col < cells.size(); col++) {
            cells.set(col, new FlyweightCell(this, col, escalator));
        }
    }

    /**
     * Get flyweight cells for the client code to render.
     * 
     * @return a list of {@link FlyweightCell FlyweightCells}. They are
     *         generified into {@link Cell Cells}, because Java's generics
     *         system isn't expressive enough.
     * @see #setup(Element, int)
     * @see #teardown()
     */
    Iterable<Cell> getCells() {
        assertSetup();
        return new Iterable<Cell>() {
            @Override
            public Iterator<Cell> iterator() {
                return new CellIterator(cells);
            }
        };
    }

    /**
     * Asserts that the flyweight row has properly been set up before trying to
     * access any of its data.
     */
    private void assertSetup() {
        assert element != null && row != BLANK && columnWidths != null : "Flyweight row was not "
                + "properly initialized. Make sure the setup-method is "
                + "called before retrieving data. This is either a bug "
                + "in Escalator, or the instance of the flyweight row "
                + "has been stored and accessed.";
    }

    int getColumnWidth(int column) {
        assertSetup();
        return columnWidths[column];
    }
}
