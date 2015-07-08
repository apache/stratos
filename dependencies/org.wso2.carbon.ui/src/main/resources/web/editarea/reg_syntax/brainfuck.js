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

editAreaLoader.load_syntax["brainfuck"] = {
	'DISPLAY_NAME' : 'Brainfuck'
	,'COMMENT_SINGLE' : {}
	,'COMMENT_MULTI' : {}
	,'QUOTEMARKS' : {}
	,'KEYWORD_CASE_SENSITIVE' : true
	,'OPERATORS' :[
		'+', '-'
	]
	,'DELIMITERS' :[
		'[', ']'
	]
	,'REGEXPS' : {
		'bfispis' : {
			'search' : '()(\\.)()'
			,'class' : 'bfispis'
			,'modifiers' : 'g'
			,'execute' : 'before'
		}
		,'bfupis' : {
			'search' : '()(\\,)()'
			,'class' : 'bfupis'
			,'modifiers' : 'g'
			,'execute' : 'before'
		}
		,'bfmemory' : {
			'search' : '()([<>])()'
			,'class' : 'bfmemory'
			,'modifiers' : 'g'
			,'execute' : 'before'
		}
	}
	,'STYLES' : {
		'COMMENTS': 'color: #AAAAAA;'
		,'QUOTESMARKS': 'color: #6381F8;'
		,'OPERATORS' : 'color: #88AA00;'
		,'DELIMITERS' : 'color: #00C138;'
		,'REGEXPS' : {
			'bfispis' : 'color: #EE0000;'
			,'bfupis' : 'color: #4455ee;'
			,'bfmemory' : 'color: #DD00DD;'
		}
	}
};

