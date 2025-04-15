/**
 * ByteBuffer类
 * 用于处理二进制数据的读写
 */
ByteBuffer = function (arrayBuf, offset) {
    var Type_Byte = 1;
    var Type_Short = 2;
    var Type_UShort = 3;
    var Type_Int32 = 4;
    var Type_UInt32 = 5;
    var Type_String = 6; // 变长字符串，前两个字节表示长度
    var Type_VString = 7; // 定长字符串
    var Type_Int64 = 8;
    var Type_Float = 9;
    var Type_Double = 10;
    var Type_ByteArray = 11;

    var _org_buf = arrayBuf ? (arrayBuf.constructor == DataView ? arrayBuf : (arrayBuf.constructor == Uint8Array ? new DataView(arrayBuf.buffer, offset) : new DataView(arrayBuf, offset))) : new DataView(new Uint8Array([]).buffer);
    var _offset = offset || 0;
    var _list = [];
    var _littleEndian = false;

    // 指定字节序 为BigEndian
    this.bigEndian = function () {
        _littleEndian = false;
        return this;
    };

    // 指定字节序 为LittleEndian
    this.littleEndian = function () {
        _littleEndian = true;
        return this;
    };

    if (!ArrayBuffer.prototype.slice) {
        ArrayBuffer.prototype.slice = function (start, end) {
            var that = new Uint8Array(this);
            if (end == undefined) end = that.length;
            var result = new ArrayBuffer(end - start);
            var resultArray = new Uint8Array(result);
            for (var i = 0; i < resultArray.length; i++)
                resultArray[i] = that[i + start];
            return result;
        }
    }

    function utf8Write(view, offset, str) {
        var c = 0;
        for (var i = 0, l = str.length; i < l; i++) {
            c = str.charCodeAt(i);
            if (c < 0x80) {
                view.setUint8(offset++, c);
            } else if (c < 0x800) {
                view.setUint8(offset++, 0xc0 | (c >> 6));
                view.setUint8(offset++, 0x80 | (c & 0x3f));
            } else if (c < 0xd800 || c >= 0xe000) {
                view.setUint8(offset++, 0xe0 | (c >> 12));
                view.setUint8(offset++, 0x80 | (c >> 6) & 0x3f);
                view.setUint8(offset++, 0x80 | (c & 0x3f));
            } else {
                i++;
                c = 0x10000 + (((c & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff));
                view.setUint8(offset++, 0xf0 | (c >> 18));
                view.setUint8(offset++, 0x80 | (c >> 12) & 0x3f);
                view.setUint8(offset++, 0x80 | (c >> 6) & 0x3f);
                view.setUint8(offset++, 0x80 | (c & 0x3f));
            }
        }
    }

    function utf8Read(view, offset, length) {
        var string = '', chr = 0;
        for (var i = offset, end = offset + length; i < end; i++) {
            var byte = view.getUint8(i);
            if ((byte & 0x80) === 0x00) {
                string += String.fromCharCode(byte);
                continue;
            }
            if ((byte & 0xe0) === 0xc0) {
                string += String.fromCharCode(
                    ((byte & 0x0f) << 6) |
                    (view.getUint8(++i) & 0x3f)
                );
                continue;
            }
            if ((byte & 0xf0) === 0xe0) {
                string += String.fromCharCode(
                    ((byte & 0x0f) << 12) |
                    ((view.getUint8(++i) & 0x3f) << 6) |
                    ((view.getUint8(++i) & 0x3f) << 0)
                );
                continue;
            }
            if ((byte & 0xf8) === 0xf0) {
                chr = ((byte & 0x07) << 18) |
                    ((view.getUint8(++i) & 0x3f) << 12) |
                    ((view.getUint8(++i) & 0x3f) << 6) |
                    ((view.getUint8(++i) & 0x3f) << 0);
                if (chr >= 0x010000) { // surrogate pair
                    chr -= 0x010000;
                    string += String.fromCharCode((chr >>> 10) + 0xD800, (chr & 0x3FF) + 0xDC00);
                } else {
                    string += String.fromCharCode(chr);
                }
                continue;
            }
            throw new Error('Invalid byte ' + byte.toString(16));
        }
        return string;
    }

    function utf8Length(str) {
        var c = 0, length = 0;
        for (var i = 0, l = str.length; i < l; i++) {
            c = str.charCodeAt(i);
            if (c < 0x80) {
                length += 1;
            } else if (c < 0x800) {
                length += 2;
            } else if (c < 0xd800 || c >= 0xe000) {
                length += 3;
            } else {
                i++;
                length += 4;
            }
        }
        return length;
    }

    this.byte = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getUint8(_offset, _littleEndian));
            _offset += 1;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_Byte, d: val, l: 1 });
            _offset += 1;
        }
        return this;
    };

    this.short = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getInt16(_offset, _littleEndian));
            _offset += 2;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_Short, d: val, l: 2 });
            _offset += 2;
        }
        return this;
    };

    this.ushort = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getUint16(_offset, _littleEndian));
            _offset += 2;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_UShort, d: val, l: 2 });
            _offset += 2;
        }
        return this;
    };

    this.int32 = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getInt32(_offset, _littleEndian));
            _offset += 4;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_Int32, d: val, l: 4 });
            _offset += 4;
        }
        return this;
    };

    this.uint32 = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getUint32(_offset, _littleEndian));
            _offset += 4;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_UInt32, d: val, l: 4 });
            _offset += 4;
        }
        return this;
    };

    /**
     * 新加的方法，获取bytebuffer的长度
     */
    this.blength = function () {
        return _offset;
    };

    /**
     * 变长字符串 前4个字节表示字符串长度
     **/
    this.string = function (val, index) {
        if (arguments.length == 0) {
            var len = _org_buf.getInt32(_offset, _littleEndian);
            _offset += 4;
            _list.push(utf8Read(_org_buf, _offset, len));
            _offset += len;
        } else {
            var len = 0;
            if (val) {
                len = utf8Length(val);
            }
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_String, d: val, l: len });
            _offset += len + 4;
        }
        return this;
    };

    /**
     * 定长字符串 val为null时，读取定长字符串（需指定长度len）
     **/
    this.vstring = function (val, len, index) {
        if (!len) {
            throw new Error('vstring must got len argument');
            return this;
        }
        if (val == undefined || val == null) {
            var vlen = 0;//实际长度
            for (var i = _offset; i < _offset + len; i++) {
                if (_org_buf.getUint8(i) > 0) vlen++;
            }
            _list.push(utf8Read(_org_buf, _offset, vlen));
            _offset += len;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_VString, d: val, l: len });
            _offset += len;
        }
        return this;
    };

    this.int64 = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getFloat64(_offset, _littleEndian));
            _offset += 8;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_Int64, d: val, l: 8 });
            _offset += 8;
        }
        return this;
    };

    this.float = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getFloat32(_offset, _littleEndian));
            _offset += 4;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_Float, d: val, l: 4 });
            _offset += 4;
        }
        return this;
    };

    this.double = function (val, index) {
        if (arguments.length == 0) {
            _list.push(_org_buf.getFloat64(_offset, _littleEndian));
            _offset += 8;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_Double, d: val, l: 8 });
            _offset += 8;
        }
        return this;
    };

    /**
     * 写入或读取一段字节数组
     **/
    this.byteArray = function (val, len, index) {
        if (!len) {
            throw new Error('byteArray must got len argument');
            return this;
        }
        if (val == undefined || val == null) {
            var arr = new Uint8Array(_org_buf.buffer.slice(_offset, _offset + len));
            _list.push(arr);
            _offset += len;
        } else {
            _list.splice(index != undefined ? index : _list.length, 0, { t: Type_ByteArray, d: val, l: len });
            _offset += len;
        }
        return this;
    };

    /**
     * 解包成数据数组
     **/
    this.unpack = function () {
        return _list;
    };

    /**
     * 打包成二进制,在前面加上4个字节表示包长
     **/
    this.packWithHead = function () {
        return this.pack(true);
    };

    /**
     * 打包成二进制
     * @param ifHead 是否在前面加上4个字节表示包长
     **/
    this.pack = function (ifHead) {
        _org_buf = new DataView(new ArrayBuffer((ifHead) ? _offset + 4 : _offset));
        var offset = 0;
        if (ifHead) {
            _org_buf.setUint32(offset, _offset, _littleEndian);
            offset += 4;
        }
        for (var i = 0; i < _list.length; i++) {
            switch (_list[i].t) {
                case Type_Byte:
                    _org_buf.setInt8(offset, _list[i].d);
                    offset += _list[i].l;
                    break;
                case Type_Short:
                    _org_buf.setInt16(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_UShort:
                    _org_buf.setUint16(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_Int32:
                    _org_buf.setInt32(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_UInt32:
                    _org_buf.setUint32(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_String:
                    //前4个字节表示字符串长度
                    _org_buf.setUint32(offset, _list[i].l, _littleEndian);
                    offset += 4;
                    utf8Write(_org_buf, offset, _list[i].d);
                    offset += _list[i].l;
                    break;
                case Type_VString:
                    utf8Write(_org_buf, offset, _list[i].d);
                    var vlen = utf8Length(_list[i].d);//字符串实际长度
                    //补齐\0
                    for (var j = offset + vlen; j < offset + _list[i].l; j++) {
                        _org_buf.setUint8(j, 0);
                    }
                    offset += _list[i].l;
                    break;
                case Type_Int64:
                    _org_buf.setFloat64(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_Float:
                    _org_buf.setFloat32(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_Double:
                    _org_buf.setFloat64(offset, _list[i].d, _littleEndian);
                    offset += _list[i].l;
                    break;
                case Type_ByteArray:
                    var indx = 0;
                    for (var j = offset; j < offset + _list[i].l; j++) {
                        if (indx < _list[i].d.length) {
                            _org_buf.setUint8(j, _list[i].d[indx]);
                        } else {//不够的话，后面补齐0x00
                            _org_buf.setUint8(j, 0);
                        }
                        indx++
                    }
                    offset += _list[i].l;
                    break;
            }
        }
        return _org_buf.buffer;
    };

    /**
     * 未读数据长度
     **/
    this.getAvailable = function () {
        if (!_org_buf) return _offset;
        return _org_buf.buffer.byteLength - _offset;
    };
}; 