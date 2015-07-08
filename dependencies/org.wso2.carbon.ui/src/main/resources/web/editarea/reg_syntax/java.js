/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

editAreaLoader.load_syntax["java"] = {
	'DISPLAY_NAME' : 'Java'
	,'COMMENT_SINGLE': { 1: '//', 2: '@' }
	, 'COMMENT_MULTI': { '/*': '*/' }
	, 'QUOTEMARKS': { 1: "'", 2: '"' }
	, 'KEYWORD_CASE_SENSITIVE': true
	, 'KEYWORDS': {
	    'constants': [
			'null', 'false', 'true'
		]
		, 'types': [
			'String', 'int', 'short', 'long', 'char', 'double', 'byte',
			'float', 'static', 'void', 'private', 'boolean', 'protected',
			'public', 'const', 'class', 'final', 'abstract', 'volatile',
			'enum', 'transient', 'interface'
		]
		, 'statements': [
            'this', 'extends', 'if', 'do', 'while', 'try', 'catch', 'finally',
            'throw', 'throws', 'else', 'for', 'switch', 'continue', 'implements',
            'break', 'case', 'default', 'goto'
		]
 		, 'keywords': [
           'new', 'return', 'import', 'native', 'super', 'package', 'assert', 'synchronized',
           'instanceof', 'strictfp'
		]
	}
	, 'OPERATORS': [
		'+', '-', '/', '*', '=', '<', '>', '%', '!', '?', ':', '&'
	]
	, 'DELIMITERS': [
		'(', ')', '[', ']', '{', '}'
	]
	, 'REGEXPS': {
	    'precompiler': {
	        'search': '()(#[^\r\n]*)()'
			, 'class': 'precompiler'
			, 'modifiers': 'g'
			, 'execute': 'before'
	    }
	}
	, 'STYLES': {
	    'COMMENTS': 'color: #AAAAAA;'
		, 'QUOTESMARKS': 'color: #6381F8;'
		, 'KEYWORDS': {
		    'constants': 'color: #EE0000;'
			, 'types': 'color: #0000EE;'
			, 'statements': 'color: #60CA00;'
			, 'keywords': 'color: #48BDDF;'
		}
		, 'OPERATORS': 'color: #FF00FF;'
		, 'DELIMITERS': 'color: #0038E1;'
		, 'REGEXPS': {
		    'precompiler': 'color: #009900;'
			, 'precompilerstring': 'color: #994400;'
		}
	}
};
