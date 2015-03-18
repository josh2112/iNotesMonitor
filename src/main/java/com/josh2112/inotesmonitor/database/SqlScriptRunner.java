package com.josh2112.inotesmonitor.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.DSLContext;

public class SqlScriptRunner {
	
	private Log log = LogFactory.getLog( SqlScriptRunner.class );
	
	private List<String> sql;
	
	public SqlScriptRunner( String resource ) throws IOException, URISyntaxException {
		sql = getStatements( resource );
	}
	
	private List<String> getStatements( String resource ) throws IOException, URISyntaxException {
		List<String> lines = new ArrayList<>();
		InputStream stream = this.getClass().getResourceAsStream( resource );
		try( BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) ) ) {
			String line;
			while(( line = reader.readLine() ) != null ) lines.add( line );
		}
		
		// Trim all lines and remove blank and comment lines
		lines = lines.stream().map( String::trim ).filter( line ->
			!line.startsWith( "--" ) && !line.isEmpty() ).collect( Collectors.toList() );
		
		List<StringBuilder> statements = new ArrayList<>();
		StringBuilder currentStmt = new StringBuilder();
		statements.add( currentStmt );
		
		for( String line : lines ) {
			currentStmt.append( line );
			if( line.endsWith( ";" ) ) {
				currentStmt = new StringBuilder();
				statements.add( currentStmt );
			}
		}
		
		return statements.stream().map( StringBuilder::toString ).filter( str -> !str.isEmpty() ).collect( Collectors.toList() );
	}
	
	public void run( DSLContext context ) {
		sql.stream().forEach( stmt -> {
			log.info( "Executing: " + stmt );
			context.execute( stmt );	
		} );
	}
}
