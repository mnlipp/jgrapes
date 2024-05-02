/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.util;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Optional;

/**
 * Stores a password in such a way that it can be cleared. Automatically 
 * clears the storage if an object of this type becomes weakly reachable.
 */
public class Password {

    private static ReferenceQueue<Password> toBeCleared
        = new ReferenceQueue<>();
    private static Thread purger;
    private static final char[] EMPTY_PASSWORD = new char[0];

    @SuppressWarnings("PMD.AvoidFieldNameMatchingTypeName")
    private char[] password;
    @SuppressWarnings({ "PMD.SingularField", "unused" })
    private final WeakReference<Password> passwordRef;

    /**
     * Instantiates a new password representation.
     *
     * @param password the password
     */
    @SuppressWarnings({ "PMD.UseVarargs", "PMD.AssignmentToNonFinalStatic",
        "PMD.ArrayIsStoredDirectly" })
    public Password(char[] password) {
        synchronized (Password.class) {
            if (purger == null) {
                purger = new Thread(() -> {
                    while (true) {
                        try {
                            Reference<? extends Password> passwordRef
                                = toBeCleared.remove();
                            Optional.ofNullable(passwordRef.get())
                                .ifPresent(Password::clear);
                            passwordRef.clear();
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                });
                purger.setName("PasswordPurger");
                purger.setDaemon(true);
                purger.start();
            }
        }
        this.password = password;
        passwordRef = new WeakReference<>(this, toBeCleared);
    }

    /**
     * Clear the stored password.
     */
    public void clear() {
        for (int i = 0; i < password.length; i++) {
            password[i] = 0;
        }
        // Don't even remember its length.
        password = EMPTY_PASSWORD;
    }

    /**
     * Returns the stored password. This is returns a reference to the
     * internally used array.
     *
     * @return the char[]
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public char[] password() {
        return password;
    }

    /**
     * Compare to a given string.
     *
     * @param value the value to compare to
     * @return true, if successful
     */
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    public boolean compareTo(String value) {
        if (value == null) {
            return false;
        }
        return Arrays.equals(value.toCharArray(), password);
    }

    /**
     * Compare to a given char array.
     *
     * @param value the value to compare to
     * @return true, if successful
     */
    @SuppressWarnings({ "PMD.UseVarargs", "PMD.SimplifyBooleanReturns" })
    public boolean compareTo(char[] value) {
        if (value == null) {
            return false;
        }
        return Arrays.equals(value, password);
    }

    @Override
    @SuppressWarnings("PMD.SimplifyBooleanReturns")
    public boolean equals(Object other) {
        if (!(other instanceof Password)) {
            return false;
        }
        return compareTo(((Password) other).password);
    }

    /**
     * Passwords shouldn't be used in sets or as keys. To avoid
     * disclosing any information about the password, this method
     * always returns 0.
     *
     * @return 0
     */
    @Override
    public int hashCode() {
        return 0;
    }

    /**
     * Return "`(hidden)`". Should prevent the password from appearing
     * unintentionally in outputs.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "(hidden)";
    }
}
