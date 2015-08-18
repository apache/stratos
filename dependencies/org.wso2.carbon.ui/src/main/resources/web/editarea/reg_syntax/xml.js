/*
* last update: 2006-08-24
*/

editAreaLoader.load_syntax["xml"] = {
	'DISPLAY_NAME' : 'XML'
	,'COMMENT_SINGLE' : {}
	,'COMMENT_MULTI' : {'<!--' : '-->'}
	,'QUOTEMARKS' : {1: "'", 2: '"'}
	,'KEYWORD_CASE_SENSITIVE' : false
	,'KEYWORDS' : {
	}
	,'OPERATORS' :[
	]
	,'DELIMITERS' :[
	]
	,'REGEXPS' : {
		'xml' : {
			'search' : '()(<\\?[^>]*?\\?>)()'
			,'class' : 'xml'
			,'modifiers' : 'g'
			,'execute' : 'before' // before or after
		}
		,'cdatas' : {
			'search' : '()(<!\\[CDATA\\[.*?\\]\\]>)()'
			,'class' : 'cdata'
			,'modifiers' : 'g'
			,'execute' : 'before' // before or after
		}
		,'tags' : {
			'search' : '(<)(/?[a-z][^ \r\n\t>]*)([^>]*>)'
			,'class' : 'tags'
			,'modifiers' : 'gi'
			,'execute' : 'before' // before or after
		}
		,'attributes' : {
			'search' : '( |\n|\r|\t)([^ \r\n\t=]+)(=)'
			,'class' : 'attributes'
			,'modifiers' : 'g'
			,'execute' : 'before' // before or after
		}
	}
	,'STYLES' : {
		'COMMENTS': 'color: #AAAAAA;'
		,'QUOTESMARKS': 'color: #00c700;'
		,'KEYWORDS' : {
			}
		,'OPERATORS' : 'color: #E775F0;'
		,'DELIMITERS' : ''
		,'REGEXPS' : {
			'attributes': 'color: #fb271a;'
			,'tags': 'color: #2d20f8;'
			,'xml': 'color: #BBBADF;'
			,'cdata': 'color: #BBBA2B;'
		}
	}
};
