"""Minimal complete NBT reader/writer used by the world serializer."""

from __future__ import annotations

import gzip
import io
import struct
from dataclasses import dataclass
from pathlib import Path

END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY, LONG_ARRAY = range(13)


@dataclass
class Tag:
    kind: int
    value: object
    list_kind: int | None = None


def tag(kind, value, list_kind=None): return Tag(kind, value, list_kind)
def byte(v): return tag(BYTE, int(v))
def short(v): return tag(SHORT, int(v))
def integer(v): return tag(INT, int(v))
def long(v): return tag(LONG, int(v))
def float_tag(v): return tag(FLOAT, float(v))
def double(v): return tag(DOUBLE, float(v))
def string(v): return tag(STRING, str(v))
def byte_array(v): return tag(BYTE_ARRAY, bytes(v))
def int_array(v): return tag(INT_ARRAY, list(v))
def long_array(v): return tag(LONG_ARRAY, list(v))
def list_tag(kind, values): return tag(LIST, list(values), kind)
def compound(**values): return tag(COMPOUND, values)


def _read_exact(stream, n):
    data=stream.read(n)
    if len(data)!=n: raise EOFError("truncated NBT")
    return data


def _read_string(stream):
    n=struct.unpack(">H",_read_exact(stream,2))[0]
    return _read_exact(stream,n).decode("utf-8")


def _read_payload(stream, kind):
    if kind==BYTE: return tag(kind,struct.unpack(">b",_read_exact(stream,1))[0])
    if kind==SHORT: return tag(kind,struct.unpack(">h",_read_exact(stream,2))[0])
    if kind==INT: return tag(kind,struct.unpack(">i",_read_exact(stream,4))[0])
    if kind==LONG: return tag(kind,struct.unpack(">q",_read_exact(stream,8))[0])
    if kind==FLOAT: return tag(kind,struct.unpack(">f",_read_exact(stream,4))[0])
    if kind==DOUBLE: return tag(kind,struct.unpack(">d",_read_exact(stream,8))[0])
    if kind==STRING: return string(_read_string(stream))
    if kind==BYTE_ARRAY:
        n=struct.unpack(">i",_read_exact(stream,4))[0]; return byte_array(_read_exact(stream,n))
    if kind==INT_ARRAY:
        n=struct.unpack(">i",_read_exact(stream,4))[0]; return int_array(struct.unpack(f">{n}i",_read_exact(stream,n*4)))
    if kind==LONG_ARRAY:
        n=struct.unpack(">i",_read_exact(stream,4))[0]; return long_array(struct.unpack(f">{n}q",_read_exact(stream,n*8)))
    if kind==LIST:
        child=struct.unpack(">b",_read_exact(stream,1))[0]; n=struct.unpack(">i",_read_exact(stream,4))[0]
        return list_tag(child,[_read_payload(stream,child) for _ in range(n)])
    if kind==COMPOUND:
        values={}
        while True:
            child=struct.unpack(">b",_read_exact(stream,1))[0]
            if child==END: break
            name=_read_string(stream); values[name]=_read_payload(stream,child)
        return tag(COMPOUND,values)
    raise ValueError(f"unsupported NBT tag {kind}")


def loads(data: bytes) -> tuple[str,Tag]:
    stream=io.BytesIO(data); kind=struct.unpack(">b",_read_exact(stream,1))[0]
    if kind==END: raise ValueError("root cannot be TAG_End")
    return _read_string(stream),_read_payload(stream,kind)


def load_gzip(path: Path) -> tuple[str,Tag]:
    with gzip.open(path,"rb") as f: return loads(f.read())


def _write_string(stream,value):
    data=value.encode("utf-8"); stream.write(struct.pack(">H",len(data))); stream.write(data)


def _write_payload(stream,t: Tag):
    k,v=t.kind,t.value
    if k==BYTE: stream.write(struct.pack(">b",v))
    elif k==SHORT: stream.write(struct.pack(">h",v))
    elif k==INT: stream.write(struct.pack(">i",v))
    elif k==LONG: stream.write(struct.pack(">q",v))
    elif k==FLOAT: stream.write(struct.pack(">f",v))
    elif k==DOUBLE: stream.write(struct.pack(">d",v))
    elif k==STRING: _write_string(stream,v)
    elif k==BYTE_ARRAY: stream.write(struct.pack(">i",len(v))); stream.write(v)
    elif k==INT_ARRAY:
        stream.write(struct.pack(">i",len(v))); stream.write(struct.pack(f">{len(v)}i",*v))
    elif k==LONG_ARRAY:
        stream.write(struct.pack(">i",len(v))); stream.write(struct.pack(f">{len(v)}q",*[_signed64(x) for x in v]))
    elif k==LIST:
        stream.write(struct.pack(">bi",t.list_kind,len(v)))
        for child in v: _write_payload(stream,child)
    elif k==COMPOUND:
        for name,child in v.items():
            stream.write(struct.pack(">b",child.kind)); _write_string(stream,name); _write_payload(stream,child)
        stream.write(b"\x00")
    else: raise ValueError(f"unsupported NBT tag {k}")


def _signed64(value):
    value &= (1<<64)-1
    return value-(1<<64) if value >= 1<<63 else value


def dumps(name: str, root: Tag) -> bytes:
    stream=io.BytesIO(); stream.write(struct.pack(">b",root.kind)); _write_string(stream,name); _write_payload(stream,root)
    return stream.getvalue()


def dump_gzip(path: Path,name: str,root: Tag):
    with path.open("wb") as raw:
        with gzip.GzipFile(fileobj=raw,mode="wb",mtime=0) as f: f.write(dumps(name,root))


def plain(t: Tag):
    if t.kind==COMPOUND: return {k:plain(v) for k,v in t.value.items()}
    if t.kind==LIST: return [plain(v) for v in t.value]
    return t.value
