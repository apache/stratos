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

/*
 * JsMin
 * Javascript Compressor
 * http://www.crockford.com/
 * http://www.smallsharptools.com/
*/

dp.sh.Brushes.Ruby=function()
{var keywords='alias and BEGIN begin break case class def define_method defined do each else elsif '+'END end ensure false for if in module new next nil not or raise redo rescue retry return '+'self super then throw true undef unless until when while yield';var builtins='Array Bignum Binding Class Continuation Dir Exception FalseClass File::Stat File Fixnum Fload '+'Hash Integer IO MatchData Method Module NilClass Numeric Object Proc Range Regexp String Struct::TMS Symbol '+'ThreadGroup Thread Time TrueClass'
this.regexList=[{regex:dp.sh.RegexLib.SingleLinePerlComments,css:'comment'},{regex:dp.sh.RegexLib.DoubleQuotedString,css:'string'},{regex:dp.sh.RegexLib.SingleQuotedString,css:'string'},{regex:new RegExp(':[a-z][A-Za-z0-9_]*','g'),css:'symbol'},{regex:new RegExp('(\\$|@@|@)\\w+','g'),css:'variable'},{regex:new RegExp(this.GetKeywords(keywords),'gm'),css:'keyword'},{regex:new RegExp(this.GetKeywords(builtins),'gm'),css:'builtin'}];this.CssClass='dp-rb';this.Style='.dp-rb .symbol { color: #a70; }'+'.dp-rb .variable { color: #a70; font-weight: bold; }';}
dp.sh.Brushes.Ruby.prototype=new dp.sh.Highlighter();dp.sh.Brushes.Ruby.Aliases=['ruby','rails','ror'];
