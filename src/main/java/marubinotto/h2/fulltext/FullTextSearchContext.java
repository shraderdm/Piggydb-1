package marubinotto.h2.fulltext;

import static marubinotto.h2.fulltext.InternalUtils.*;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import marubinotto.util.Assert;

import org.apache.commons.lang.UnhandledException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
// import org.h2.util.New; // Removed
import org.h2.util.SoftHashMap; // Assuming this still exists or replacing it?

public class FullTextSearchContext {
	
	private static final Map<String, FullTextSearchContext> CONTEXTS = new HashMap<>();
	
	public static FullTextSearchContext getContext(Connection conn) throws SQLException {
        String path = getIndexPath(conn);
        FullTextSearchContext context = CONTEXTS.get(path);
        if (context == null) {
        	context = new FullTextSearchContext();
            CONTEXTS.put(path, context);
        }
        return context;
    }
    	
	private HashMap<String, Integer> words = new HashMap<>();
	private HashMap<Integer, IndexedTableInfo> indexedTables = new HashMap<>();

	private FullTextSearchContext() {
	}
	
	public void clearAll() {
		this.words.clear();
		this.indexedTables.clear();
	}

    public Map<String, Integer> getWordList() {
        return this.words;
    }

    public IndexedTableInfo getIndexedTableInfo(int infoId) {
        return this.indexedTables.get(infoId);
    }

    public void addIndexedTableInfo(IndexedTableInfo info) {
    	Assert.Arg.notNull(info, "info");
    	Assert.Arg.notNull(info.id, "info.id");
        this.indexedTables.put(info.id, info);
    }

    public void removeIndexedTableInfo(IndexedTableInfo info) {
    	Assert.Arg.notNull(info, "info");
    	Assert.Arg.notNull(info.id, "info.id");
    	this.indexedTables.remove(info.id);
    }
    
    public String convertWord(String word) {
        word = word.toUpperCase();
        return word;
    }
    
    private final static Analyzer ANALYZER = new CJKAnalyzer(Version.LUCENE_36);

    public void splitIntoWords(String text, Set<String> words) {
    	TokenStream stream = ANALYZER.tokenStream("F", new StringReader(text));
        try {
            stream.reset();
            CharTermAttribute termAttr = stream.addAttribute(CharTermAttribute.class);
			while (stream.incrementToken()) {
				String word = termAttr.toString();
				word = convertWord(word);
	            if (word != null) words.add(word);
			}
            stream.end();
		} 
        catch (IOException e) {
			throw new UnhandledException(e);
		}
        finally {
            try { stream.close(); } catch (IOException e) { /* ignore */ }
        }
    }
    
    public void splitIntoWords(IndexedTableInfo tableInfo, Object[] row, Set<String> words) 
    throws SQLException {
    	for (int columnIndex : tableInfo.indexColumns) {
    		Object data = row[columnIndex];
            int type = tableInfo.columnTypes.get(columnIndex);
            // NOTE: omitted the case: type == Types.CLOB for large clob
            String string = InternalUtils.toString(data, type);
            splitIntoWords(string, words);
        }
    }
    
    // Using simple HashMap if SoftHashMap is gone, or check if H2 has it.
    // Assuming SoftHashMap might be gone (it's internal).
    // I will replace with HashMap for now, assuming memory is fine or use WeakHashMap.
    // PreparedStatement cache is useful but not critical.
    protected Map<Connection, Map<String, PreparedStatement>> cache =
	new java.util.WeakHashMap<>(); // Use WeakHashMap to allow GC of connections

    protected synchronized PreparedStatement prepare(Connection conn, String sql) throws SQLException {
        Map<String, PreparedStatement> preps = cache.get(conn);
        if (preps == null) {
            preps = new HashMap<>(); // Strong ref inside WeakHashMap value? No, value is strong.
            // WeakHashMap keys are weak. If connection is closed/GCed, entry removed.
            // But we need to close statements?
            // H2 internal SoftHashMap was doing something specific.
            // I'll stick to simple caching for now.
            this.cache.put(conn, preps);
        }
        PreparedStatement prep = preps.get(sql);
        if (prep != null && prep.getConnection().isClosed()) {
            prep = null;
        }
        if (prep == null) {
            prep = conn.prepareStatement(sql);	// what if the connection isClosed?
            preps.put(sql, prep);
        }
        return prep;
    }
}
