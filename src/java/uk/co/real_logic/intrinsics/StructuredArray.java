/*
 * Copyright 2012 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.intrinsics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <p>
 *      An array of structured types that behaves like an array of structures in the C language.
 *      The length of the array is a signed 64-bit value.
 * </p>
 * <p>
 *      A JVM can optimise the implementation to provide a compact contiguous layout
 *      that facilitates consistent stride based memory access.
 * </p>
 * @param <E> structured type occupying each element.
 */
public class StructuredArray<E> implements Iterable<E>
{
    private static final Class[] EMPTY_INIT_ARG_TYPES = new Class[0];
    private static final Object[] EMPTY_INIT_ARGS = new Object[0];

    private static final int PARTITION_POWER_OF_TWO = 30;
    private static final int MAX_PARTITION_SIZE = 1 << PARTITION_POWER_OF_TWO;
    private static final int MASK = MAX_PARTITION_SIZE  - 1;

    private final long length;
    private final E[][] partitions;
    private final Field[] fields;
    private final Class<E> componentClass;

    /**
     * Create an array of types to be laid out like a contiguous array of structures.
     *
     * @param length of the array to create.
     * @param componentClass of each element in the array
     */
    public StructuredArray(final long length, final Class<E> componentClass)
    {
        this(length, componentClass, EMPTY_INIT_ARG_TYPES, EMPTY_INIT_ARGS);
    }

    /**
     * Create an array of types to be laid out like a contiguous array of structures and provide
     * a list of arguments to be passed to a constructor for each element.
     *
     * @param length of the array to create.
     * @param componentClass of each element in the array
     * @param initArgTypes for selecting the constructor to call for initialising each structure object.
     * @param initArgs to be passed to a constructor for initialising each structure object.
     * @throws IllegalArgumentException if the constructor arguments do not match the signature.
     */
    @SuppressWarnings("unchecked")
    public StructuredArray(final long length, final Class<E> componentClass,
                           final Class[] initArgTypes, final Object... initArgs)
    {
        if (length < 0)
        {
            throw new IllegalArgumentException("length cannot be negative");
        }

        if (null == componentClass)
        {
            throw new NullPointerException("componentClass cannot be null");
        }

        if (initArgTypes.length != initArgs.length)
        {
            throw new IllegalArgumentException("argument types and values must be the same length");
        }

        final Constructor<E> ctor = findConstructor(componentClass, initArgTypes);

        this.length = length;
        this.componentClass = componentClass;
        this.fields = componentClass.getDeclaredFields();
        for (final Field field : fields)
        {
            field.setAccessible(true);
        }

        final int numFullPartitions = (int)(length / MAX_PARTITION_SIZE);
        final int lastPartitionSize = (int)(length % MAX_PARTITION_SIZE);

        partitions = (E[][])new Object[numFullPartitions + 1][];
        for (int i = 0; i < numFullPartitions; i++)
        {
            partitions[i] = (E[])new Object[MAX_PARTITION_SIZE];
        }
        partitions[numFullPartitions] = (E[])new Object[lastPartitionSize];

        populatePartitions(ctor, initArgs);
    }

    /**
     * Get the length of the array by number of elements.
     *
     * @return the number of elements in the array.
     */
    public long getLength()
    {
        return length;
    }

    /**
     * Get the {@link Class} of elements stored as components of the array.
     *
     * @return the {@link Class} of elements stored as components of the array.
     */
    public Class<E> getComponentClass()
    {
        return componentClass;
    }

    /**
     * Get a reference to an element in the array.
     *
     * @param index of the element to retrieve.
     * @return a reference to the indexed element.
     */
    public E get(final long index)
    {
        final int partitionIndex = (int)(index >>> PARTITION_POWER_OF_TWO);
        final int partitionOffset = (int)index & MASK;

        return partitions[partitionIndex][partitionOffset];
    }

    /**
     * Shallow copy a region of structures from one array to the other.  If the same array is both the src
     * and dst then the copy will happen as if a temporary intermediate array was used.
     *
     * @param src array to copy.
     * @param srcOffset offset index in src where the region begins.
     * @param dst array into which the copy should occur.
     * @param dstOffset offset index in the dst where the region begins.
     * @param count of structure elements to copy.
     * @param allowFinalFieldOverwrite allow final fields to be overwritten during a copy operation.
     * @throws IllegalStateException if final fields are discovered and all allowFinalFieldOverwrite is not true.
     * @throws ArrayStoreException if the {@link StructuredArray#getComponentClass()}s are not identical.
     */
    public static void shallowCopy(final StructuredArray src, final long srcOffset,
                                   final StructuredArray dst, final long dstOffset,
                                   final long count, final boolean allowFinalFieldOverwrite)
    {
        if (src.componentClass != dst.componentClass)
        {
            final String msg = String.format("Only objects of the same class can be copied: %s != %s",
                                             src.getClass(), dst.getClass());

            throw new ArrayStoreException(msg);
        }

        final Field[] fields = src.fields;
        if (!allowFinalFieldOverwrite)
        {
            checkForFinalFields(fields);
        }

        if (dst == src && (dstOffset >= srcOffset && (dstOffset + count) >= srcOffset))
        {
            for (long srcIdx = srcOffset + count, dstIdx = dstOffset + count, limit = srcOffset - 1;
                 srcIdx > limit;
                 srcIdx--, dstIdx--)
            {
                reverseShallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
            }
        }
        else
        {
            for (long srcIdx = srcOffset, dstIdx = dstOffset, limit = srcOffset + count;
                 srcIdx < limit;
                 srcIdx++, dstIdx++)
            {
                shallowCopy(src.get(srcIdx), dst.get(dstIdx), fields);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public StructureIterator iterator()
    {
        return new StructureIterator();
    }

    /**
     * Specialised {@link Iterator} with the ability to be {@link #reset()} enabling reuse.
     */
    public class StructureIterator implements Iterator<E>
    {
        private long cursor = 0;

        /**
         * {@inheritDoc}
         */
        public boolean hasNext()
        {
            return cursor < length;
        }

        /**
         * {@inheritDoc}
         */
        public E next()
        {
            if (cursor >= length)
            {
                throw new NoSuchElementException();
            }

            return get(cursor++);
        }

        /**
         * Remove operations are not supported on {@link StructuredArray}s.
         *
         * @throws UnsupportedOperationException if called.
         */
        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * Reset to the beginning of the collection enabling reuse of the iterator.
         */
        public void reset()
        {
            cursor = 0;
        }
    }

    private Constructor<E> findConstructor(final Class<E> componentClass, final Class[] argTypes)
    {
        final Constructor<E> ctor;
        try
        {
            ctor = componentClass.getConstructor(argTypes);
        }
        catch (final NoSuchMethodException ex)
        {
            throw new IllegalArgumentException(ex);
        }

        return ctor;
    }

    private void populatePartitions(final Constructor<E> ctor, final Object[] initArgs)
    {
        try
        {
            for (final E[] partition : partitions)
            {
                for (int i = 0, size = partition.length; i < size; i++)
                {
                    partition[i] = ctor.newInstance(initArgs);
                }
            }
        }
        catch (final Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    private static void checkForFinalFields(final Field[] fields)
    {
        for (final Field field : fields)
        {
            if (Modifier.isFinal(field.getModifiers()))
            {
                throw new IllegalStateException("Final fields should not be overwritten");
            }
        }
    }

    private static void shallowCopy(final Object src, final Object dst, final Field[] fields)
    {
        try
        {
            for (final Field field : fields)
            {
                field.set(dst, field.get(src));
            }
        }
        catch (final IllegalAccessException shouldNotHappen)
        {
            throw new RuntimeException(shouldNotHappen);
        }
    }

    private static void reverseShallowCopy(final Object src, final Object dst, final Field[] fields)
    {
        try
        {
            for (int i = fields.length - 1; i >= 0; i--)
            {
                final Field field = fields[i];
                field.set(dst, field.get(src));
            }
        }
        catch (final IllegalAccessException shouldNotHappen)
        {
            throw new RuntimeException(shouldNotHappen);
        }
    }
}
