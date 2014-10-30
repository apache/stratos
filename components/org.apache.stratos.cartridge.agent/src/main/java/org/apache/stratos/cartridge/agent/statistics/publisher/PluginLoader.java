package org.apache.stratos.cartridge.agent.statistics.publisher;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;
import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;
import java.util.jar.*;
import java.util.zip.*;


public class PluginLoader
{
    private static final Log log = LogFactory.getLog(PluginLoader.class);

    /* Built-in plugins are listed here. This is just easier than adding this
     * jar to the list of jars to search, as it contains too much random stuff,
     * including stuff with unsatisfied dependencies that we otherwise wouldn't
     * load. There's a nice looking library called Reflections on Google Code
     * that would probably be OK, but it's more logic.
     */

    public static List<Class> loadPluginClassesFromJar( File jarPath, Class pluginInterface )
    {
        List<Class> listeners = new LinkedList<Class>( );

        try
        {
            URLClassLoader loader = new URLClassLoader( new URL[] { jarPath.toURI().toURL() } );
            JarFile jar = new JarFile( jarPath );
            Enumeration<? extends JarEntry> jarEnum = jar.entries();

            log.trace( "Scanning jar file " + jarPath );

            while( jarEnum.hasMoreElements() )
            {
                ZipEntry zipEntry = jarEnum.nextElement();
                String fileName = zipEntry.getName();

                if( fileName.endsWith( ".class" ) )
                {
                    log.trace( "Considering jar entry " + fileName );
                    try
                    {
                        String className = fileName.replace( ".class", "" ).replace( "/", "." );
                        Class cls = loader.loadClass( className );
                        log.trace( "Loaded class " + className );

                        if( hasInterface( cls, pluginInterface ) )
                        {
                            log.trace( "Class has " + pluginInterface.getName()  + " interface; adding " );
                            listeners.add( cls );
                        }
                    }
                    catch( ClassNotFoundException e )
                    {
                        log.error( "Unable to load class from " + fileName + " in " + jarPath );
                    }
                }
            }

        }
        catch( IOException e )
        {
            log.error( "Unable to open JAR file " + jarPath, e );
        }

        return listeners;
    }

    private static boolean hasInterface( Class cls, Class iface )
    {
        for( Class in : cls.getInterfaces() )
        {
            if( in == iface )
            {
                return true;
            }
        }
        return false;
    }
}
