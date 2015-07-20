/**
 * The MIT License
 * Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.tealcube.minecraft.bukkit.tenacity;

import com.tealcube.minecraft.bukkit.lumberjack.shade.slf4j.Logger;
import com.tealcube.minecraft.bukkit.lumberjack.shade.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Deque;

public final class CloseableRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloseableRegistry.class);

    // Preallocate space for 4 closeables
    private final Deque<Closeable> registry = new ArrayDeque<>(4);

    public <C extends Closeable> C register(C closeable) {
        LOGGER.debug("Registering a Closeable");
        registry.push(closeable);
        return closeable;
    }

    public <C extends Connection> C register(final C connection) {
        registry.push(new Closeable() {
            @Override
            public void close() throws IOException {
                try {
                    connection.close();
                } catch (SQLException e) {
                    LOGGER.info("unable to close connection", e);
                    throw new IOException(e);
                }
            }
        });
        return connection;
    }

    public <C extends Statement> C register(final C statement) {
        registry.push(new Closeable() {
            @Override
            public void close() throws IOException {
                try {
                    statement.close();
                } catch (SQLException e) {
                    LOGGER.info("unable to close statement", e);
                    throw new IOException(e);
                }
            }
        });
        return statement;
    }

    public <C extends ResultSet> C register(final C resultSet) {
        registry.push(new Closeable() {
            @Override
            public void close() throws IOException {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    LOGGER.info("unable to close ResultSet", e);
                    throw new IOException(e);
                }
            }
        });
        return resultSet;
    }

    public void close() throws IOException {
        while (!registry.isEmpty()) {
            Closeable closeable = registry.pop();
            try {
                closeable.close();
            } catch (IOException e) {
                LOGGER.info("unable to close something during close()");
                throw new IOException(e);
            }
        }
    }

    public void closeQuietly() {
        try {
            close();
        } catch (IOException e) {
            LOGGER.info("an exception was thrown while closing quietly", e);
        }
    }

}
