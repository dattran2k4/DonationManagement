#!/usr/bin/env python3
from __future__ import annotations

import html
import json
import re
import subprocess
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import date
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "src" / "main" / "java" / "com" / "chiaseyeuthuong"
COMMON_DIR = SRC / "common"
API_DIR = SRC / "api"
DTO_REQUEST_DIR = SRC / "dto" / "request"
DTO_RESPONSE_DIR = SRC / "dto" / "response"
SERVICE_DIR = SRC / "service"
APPLICATION_YML = ROOT / "src" / "main" / "resources" / "application.yml"
OUTPUT_DIR = ROOT / "docs"
OUTPUT_HTML = OUTPUT_DIR / "Dac_ta_API_DonationManagement.html"
OUTPUT_DOCX = OUTPUT_DIR / "Dac_ta_API_DonationManagement.docx"


MODULE_ORDER = [
    "Danh mục",
    "Sự kiện",
    "Hoạt động",
    "Nhà hảo tâm",
    "Quyên góp",
    "Thanh toán",
    "Dashboard",
    "Người dùng",
    "Cấu hình hệ thống",
    "Giao dịch",
    "Nhật ký hệ thống",
    "Nhập xuất Excel",
    "Webhook",
]


MODULE_TITLES = {
    "ApiCategoryController": "Danh mục",
    "ApiEventController": "Sự kiện",
    "ApiActivityController": "Hoạt động",
    "ApiDonorController": "Nhà hảo tâm",
    "ApiDonorDonationController": "Nhà hảo tâm",
    "ApiDonationController": "Quyên góp",
    "ApiPaymentController": "Thanh toán",
    "ApiDashboardController": "Dashboard",
    "ApiUserController": "Người dùng",
    "ApiSystemConfigController": "Cấu hình hệ thống",
    "ApiTransactionController": "Giao dịch",
    "ApiAuditLogController": "Nhật ký hệ thống",
    "ApiAdminExcelController": "Nhập xuất Excel",
    "ApiWebhookController": "Webhook",
}


MODULE_CODES = {
    "Danh mục": "CAT",
    "Sự kiện": "EVT",
    "Hoạt động": "ACT",
    "Nhà hảo tâm": "DNR",
    "Quyên góp": "DNT",
    "Thanh toán": "PAY",
    "Dashboard": "DSH",
    "Người dùng": "USR",
    "Cấu hình hệ thống": "CFG",
    "Giao dịch": "TRX",
    "Nhật ký hệ thống": "AUD",
    "Nhập xuất Excel": "XLS",
    "Webhook": "WBH",
}


KNOWN_TYPES = {
    "String",
    "Long",
    "Integer",
    "int",
    "long",
    "boolean",
    "Boolean",
    "BigDecimal",
    "LocalDate",
    "LocalDateTime",
    "Date",
    "MultipartFile",
    "Object",
    "byte[]",
}


@dataclass
class FieldInfo:
    name: str
    type_name: str
    annotations: list[str] = field(default_factory=list)


@dataclass
class ParamInfo:
    source: str
    name: str
    type_name: str
    required: bool | None
    default: str | None
    note: str


@dataclass
class EndpointInfo:
    controller_name: str
    module_title: str
    code: str
    method_name: str
    http_method: str
    paths: list[str]
    permission: str
    title: str
    description: str
    status_code: int
    response_wrapper: str
    response_type: str | None
    request_body_type: str | None
    path_params: list[ParamInfo]
    query_params: list[ParamInfo]
    file_params: list[ParamInfo]
    content_type: str | None
    notes: list[str]


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def extract_block(lines: list[str], start_index: int) -> tuple[str, int]:
    collected = [lines[start_index].strip()]
    balance = collected[0].count("(") - collected[0].count(")")
    index = start_index
    while balance > 0 and index + 1 < len(lines):
        index += 1
        collected.append(lines[index].strip())
        balance += lines[index].count("(") - lines[index].count(")")
    return " ".join(collected), index


def clean_generic(type_name: str) -> str:
    return " ".join(type_name.replace("final ", "").split())


def split_top_level(text: str, delimiter: str = ",") -> list[str]:
    parts: list[str] = []
    current: list[str] = []
    angle = 0
    paren = 0
    brace = 0
    in_string = False
    prev = ""
    for char in text:
        if char == '"' and prev != "\\":
            in_string = not in_string
        if not in_string:
            if char == "<":
                angle += 1
            elif char == ">":
                angle = max(0, angle - 1)
            elif char == "(":
                paren += 1
            elif char == ")":
                paren = max(0, paren - 1)
            elif char == "{":
                brace += 1
            elif char == "}":
                brace = max(0, brace - 1)
            elif char == delimiter and angle == 0 and paren == 0 and brace == 0:
                parts.append("".join(current).strip())
                current = []
                prev = char
                continue
        current.append(char)
        prev = char
    tail = "".join(current).strip()
    if tail:
        parts.append(tail)
    return parts


def normalize_space(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def parse_annotation_value(annotation: str, key: str) -> str | None:
    match = re.search(rf"{re.escape(key)}\s*=\s*\"([^\"]*)\"", annotation)
    return match.group(1) if match else None


def parse_annotation_bool(annotation: str, key: str, default: bool | None = None) -> bool | None:
    match = re.search(rf"{re.escape(key)}\s*=\s*(true|false)", annotation)
    if match:
        return match.group(1) == "true"
    return default


def parse_enums() -> dict[str, list[dict[str, str | None]]]:
    enums: dict[str, list[dict[str, str | None]]] = {}
    for path in sorted(COMMON_DIR.glob("*.java")):
        text = read_text(path)
        class_match = re.search(r"enum\s+(\w+)\s*\{", text)
        if not class_match:
            continue
        enum_name = class_match.group(1)
        items: list[dict[str, str | None]] = []
        for match in re.finditer(r"^\s*([A-Z0-9_]+)(?:\(\"([^\"]+)\"\))?\s*[,;}]?", text, re.M):
            items.append({"name": match.group(1), "label": match.group(2)})
        enums[enum_name] = items
    return enums


def parse_fields(java_text: str) -> list[FieldInfo]:
    lines = java_text.splitlines()
    fields: list[FieldInfo] = []
    annotations: list[str] = []
    for index, line in enumerate(lines):
        stripped = line.strip()
        if not stripped:
            continue
        if stripped.startswith("@"):
            annotation, end_index = extract_block(lines, index)
            annotations.append(annotation)
            if end_index != index:
                for skip in range(index + 1, end_index + 1):
                    lines[skip] = ""
            continue
        if "(" in stripped or stripped.startswith("public class") or stripped.startswith("public enum") or stripped.startswith("public interface"):
            annotations = []
            continue
        field_match = re.match(
            r"^(private|protected|public)\s+([\w<>\[\], ?\.]+?)\s+(\w+)\s*(?:=.*)?;$",
            stripped,
        )
        if field_match:
            fields.append(
                FieldInfo(
                    name=field_match.group(3),
                    type_name=clean_generic(field_match.group(2)),
                    annotations=annotations[:],
                )
            )
            annotations = []
        else:
            annotations = []
    return fields


def parse_dto_fields(directory: Path) -> dict[str, list[FieldInfo]]:
    result: dict[str, list[FieldInfo]] = {}
    for path in sorted(directory.glob("*.java")):
        text = read_text(path)
        class_match = re.search(r"(?:class|record)\s+(\w+)", text)
        if not class_match:
            continue
        result[class_match.group(1)] = parse_fields(text)
    return result


def parse_service_returns() -> dict[str, dict[str, str]]:
    result: dict[str, dict[str, str]] = {}
    for path in sorted(SERVICE_DIR.glob("*.java")):
        text = read_text(path)
        interface_match = re.search(r"interface\s+(\w+)", text)
        if not interface_match:
            continue
        normalized = normalize_space(text)
        returns: dict[str, str] = {}
        for match in re.finditer(r"([\w<>\[\], ?\.]+?)\s+(\w+)\((.*?)\)\s*;", normalized):
            return_type = clean_generic(match.group(1))
            method_name = match.group(2)
            returns[method_name] = return_type
        result[interface_match.group(1)] = returns
    return result


def parse_base_url() -> str:
    text = read_text(APPLICATION_YML)
    match = re.search(r"base-url:\s*\$\{APP_BASE_URL:([^}]+)\}", text)
    if not match:
        return "http://localhost:8080"
    value = match.group(1)
    value = value.replace("${SERVER_PORT:8080", "8080")
    value = value.replace("${SERVER_PORT:8080}", "8080")
    return value


def parse_mapping_paths(annotation: str) -> list[str]:
    if "(" not in annotation:
        return [""]
    quoted = re.findall(r"\"([^\"]*)\"", annotation)
    return quoted or [""]


def parse_permission(annotation: str | None) -> str:
    if not annotation:
        return "Công khai"
    if "hasRole('ADMIN')" in annotation:
        return "Yêu cầu đăng nhập, vai trò ADMIN"
    any_role = re.search(r"hasAnyRole\((.*?)\)", annotation)
    if any_role:
        roles = [item.strip().strip("'") for item in any_role.group(1).split(",")]
        return "Yêu cầu đăng nhập, một trong các vai trò: " + ", ".join(roles)
    role = re.search(r"hasRole\('([^']+)'\)", annotation)
    if role:
        return f"Yêu cầu đăng nhập, vai trò {role.group(1)}"
    return annotation.replace("@PreAuthorize", "").strip()


def extract_string_literals(annotation: str) -> list[str]:
    return re.findall(r"\"([^\"]*)\"", annotation)


def guess_param_name(annotation: str, fallback: str) -> str:
    direct = re.match(r"@\w+\(\s*\"([^\"]+)\"\s*\)", annotation)
    if direct:
        return direct.group(1)
    named = re.search(r"(?:value|name)\s*=\s*\"([^\"]+)\"", annotation)
    if named:
        return named.group(1)
    return fallback


def describe_validation(annotations: list[str], type_name: str, enums: dict[str, list[dict[str, str | None]]]) -> str:
    descriptions: list[str] = []
    for annotation in annotations:
        if annotation.startswith("@NotBlank"):
            descriptions.append("Bắt buộc, không được để trống")
        elif annotation.startswith("@NotNull"):
            descriptions.append("Bắt buộc")
        elif annotation.startswith("@NotEmpty"):
            descriptions.append("Danh sách bắt buộc và không được rỗng")
        elif annotation.startswith("@Email"):
            descriptions.append("Định dạng email hợp lệ")
        elif annotation.startswith("@Positive"):
            descriptions.append("Giá trị phải lớn hơn 0")
        elif annotation.startswith("@Min"):
            value = parse_annotation_value(annotation, "value")
            if value:
                descriptions.append(f"Giá trị tối thiểu: {value}")
            else:
                number_match = re.search(r"@Min\((\d+)\)", annotation)
                if number_match:
                    descriptions.append(f"Giá trị tối thiểu: {number_match.group(1)}")
        elif annotation.startswith("@DecimalMin"):
            value = parse_annotation_value(annotation, "value")
            if value:
                descriptions.append(f"Giá trị nhỏ nhất: {value}")
        elif annotation.startswith("@DecimalMax"):
            value = parse_annotation_value(annotation, "value")
            if value:
                descriptions.append(f"Giá trị lớn nhất: {value}")
        elif annotation.startswith("@Pattern"):
            regexp = parse_annotation_value(annotation, "regexp")
            if regexp:
                descriptions.append(f"Phải khớp mẫu: {regexp}")
        elif annotation.startswith("@Size"):
            min_value = re.search(r"min\s*=\s*(\d+)", annotation)
            max_value = re.search(r"max\s*=\s*(\d+)", annotation)
            if min_value and max_value:
                descriptions.append(f"Độ dài từ {min_value.group(1)} đến {max_value.group(1)} ký tự")
            elif min_value:
                descriptions.append(f"Độ dài tối thiểu {min_value.group(1)} ký tự")
            elif max_value:
                descriptions.append(f"Độ dài tối đa {max_value.group(1)} ký tự")
        elif annotation.startswith("@EnumValue"):
            enum_match = re.search(r"enumClass\s*=\s*(\w+)\.class", annotation)
            if enum_match:
                enum_name = enum_match.group(1)
                values = ", ".join(item["name"] for item in enums.get(enum_name, []))
                if values:
                    descriptions.append(f"Giá trị hợp lệ: {values}")
    if not descriptions and type_name in enums:
        values = ", ".join(item["name"] for item in enums[type_name])
        if values:
            descriptions.append(f"Giá trị hợp lệ: {values}")
    return "; ".join(descriptions) or "Theo kiểu dữ liệu chuẩn của hệ thống"


def sample_string(field_name: str) -> str:
    lower = field_name.lower()
    if "email" in lower:
        return "nguyenvana@example.com"
    if "phone" in lower:
        return "0901234567"
    if "url" in lower:
        return "http://localhost:8080/uploads/sample.png"
    if "slug" in lower:
        return "chien-dich-mua-he-yeu-thuong"
    if "code" in lower:
        return "ABC123"
    if "name" in lower:
        return "Nguyen Van A"
    if "location" in lower:
        return "Da Nang"
    if "description" in lower or "content" in lower or "message" in lower or "note" in lower or "reason" in lower:
        return "Noi dung mau"
    if "address" in lower:
        return "123 Nguyen Van Linh, Da Nang"
    if "tax" in lower:
        return "0401234567"
    if "representative" in lower:
        return "Tran Thi B"
    if "memo" in lower:
        return "MEMO2026001"
    if "statuslabel" in lower or "targetlabel" in lower or "paymentmethodlabel" in lower or "periodlabel" in lower:
        return "Nhan hien thi"
    if "useragent" in lower:
        return "Mozilla/5.0"
    if "ipaddress" in lower:
        return "127.0.0.1"
    return "string"


def sample_primitive(type_name: str, field_name: str = "") -> Any:
    if type_name == "String":
        return sample_string(field_name)
    if type_name in {"Long", "long"}:
        return 1
    if type_name in {"Integer", "int"}:
        return 1
    if type_name in {"Boolean", "boolean"}:
        return True
    if type_name == "BigDecimal":
        return 500000
    if type_name == "LocalDate":
        return "2026-04-11"
    if type_name == "LocalDateTime":
        return "2026-04-11T18:00:00"
    if type_name == "Date":
        return "2026-04-11T18:00:00+07:00"
    if type_name == "byte[]":
        return "<binary>"
    return None


def sample_for_type(
    type_name: str,
    request_dtos: dict[str, list[FieldInfo]],
    response_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
    field_name: str = "",
    seen: set[str] | None = None,
    depth: int = 0,
) -> Any:
    seen = seen or set()
    type_name = clean_generic(type_name)
    primitive = sample_primitive(type_name, field_name)
    if primitive is not None:
        return primitive
    if type_name in enums:
        values = enums[type_name]
        return values[0]["name"] if values else None
    if type_name.startswith("List<") and type_name.endswith(">"):
        inner = type_name[5:-1].strip()
        return [sample_for_type(inner, request_dtos, response_dtos, enums, field_name, seen, depth + 1)]
    if type_name.startswith("Map<"):
        return {
            "sampleKey": "sampleValue",
            "anotherKey": "anotherValue",
        }
    if type_name.startswith("PageResponse<") and type_name.endswith(">"):
        inner = type_name[len("PageResponse<"):-1].strip()
        return {
            "page": 1,
            "pageSize": 10,
            "totalPages": 1,
            "totalItems": 1,
            "data": [sample_for_type(inner, request_dtos, response_dtos, enums, field_name, seen, depth + 1)],
        }
    if type_name.endswith("[]"):
        inner = type_name[:-2]
        return [sample_for_type(inner, request_dtos, response_dtos, enums, field_name, seen, depth + 1)]
    if type_name in seen or depth > 2:
        return {"id": 1}
    fields = response_dtos.get(type_name) or request_dtos.get(type_name)
    if fields:
        seen = set(seen)
        seen.add(type_name)
        sample_obj = {}
        for field in fields:
            sample_obj[field.name] = sample_for_type(
                field.type_name,
                request_dtos,
                response_dtos,
                enums,
                field.name,
                seen,
                depth + 1,
            )
        return sample_obj
    if type_name == "Object":
        return {"key": "value"}
    if type_name == "MultipartFile":
        return "<multipart file>"
    return {"id": 1, "name": sample_string(field_name or "name")}


def parse_method_title(message: str, method_name: str, paths: list[str]) -> str:
    if message and message not in {"OK", "Payment URL created"}:
        return message
    human_path = paths[0] if paths else method_name
    return f"Xử lý endpoint {human_path}"


def split_parameters(params_text: str) -> list[str]:
    params_text = params_text.strip()
    if not params_text:
        return []
    return split_top_level(params_text, ",")


def parse_single_param(param: str, enums: dict[str, list[dict[str, str | None]]]) -> tuple[ParamInfo | None, str | None]:
    if not param:
        return None, None
    annotation_matches = re.findall(r"@\w+(?:\([^)]*\))?", param)
    raw = param
    for annotation in annotation_matches:
        raw = raw.replace(annotation, "")
    raw = normalize_space(raw)
    if raw.startswith("Principal "):
        return None, None
    type_match = re.match(r"([\w<>\[\], ?\.]+?)\s+(\w+)$", raw)
    if not type_match:
        return None, None
    type_name = clean_generic(type_match.group(1))
    var_name = type_match.group(2)
    note = ""
    request_body_type = None
    for annotation in annotation_matches:
        if annotation.startswith("@RequestBody"):
            request_body_type = type_name
            return None, request_body_type
    source = "query"
    name = var_name
    required: bool | None = None
    default: str | None = None
    for annotation in annotation_matches:
        if annotation.startswith("@PathVariable"):
            source = "path"
            name = guess_param_name(annotation, var_name)
            required = parse_annotation_bool(annotation, "required", True)
        elif annotation.startswith("@RequestParam"):
            source = "file" if type_name == "MultipartFile" else "query"
            name = guess_param_name(annotation, var_name)
            required = parse_annotation_bool(annotation, "required", True)
            default = parse_annotation_value(annotation, "defaultValue")
        elif annotation.startswith("@Min"):
            number = re.search(r"@Min\((\d+)\)", annotation)
            if number:
                note = f"Giá trị tối thiểu {number.group(1)}"
        elif annotation.startswith("@DateTimeFormat"):
            note = "Định dạng ISO 8601"
    if type_name in enums:
        values = ", ".join(item["name"] for item in enums[type_name])
        note = f"{note}; Giá trị hợp lệ: {values}".strip("; ")
    if default is not None:
        note = f"{note}; Mặc định: {default}".strip("; ")
    return ParamInfo(source=source, name=name, type_name=type_name, required=required, default=default, note=note or "Theo kiểu dữ liệu chuẩn"), None


def extract_method_body(text: str, start_index: int) -> tuple[str, int]:
    brace_index = text.find("{", start_index)
    if brace_index == -1:
        return "", start_index
    depth = 1
    index = brace_index + 1
    while index < len(text) and depth > 0:
        if text[index] == "{":
            depth += 1
        elif text[index] == "}":
            depth -= 1
        index += 1
    return text[brace_index + 1:index - 1], index


def parse_controller(
    path: Path,
    service_returns: dict[str, dict[str, str]],
    enums: dict[str, list[dict[str, str | None]]],
) -> list[EndpointInfo]:
    text = read_text(path)
    controller_name = path.stem
    module_title = MODULE_TITLES.get(controller_name, controller_name)
    class_match = re.search(r'@RequestMapping\("([^"]+)"\)', text)
    base_path = class_match.group(1) if class_match else ""
    field_services = {
        match.group(2): match.group(1)
        for match in re.finditer(r"private final (\w+) (\w+);", text)
    }
    endpoints: list[EndpointInfo] = []
    lines = text.splitlines()
    annotations: list[str] = []
    method_counter = 0
    index = 0
    while index < len(lines):
        line = lines[index]
        stripped = line.strip()
        if stripped.startswith("@"):
            annotation, end_index = extract_block(lines, index)
            annotations.append(annotation)
            index = end_index + 1
            continue
        signature_match = re.search(r"public\s+([\w<>\[\], ?\.]+?)\s+(\w+)\((.*)", line)
        if not signature_match:
            index += 1
            continue
        signature_lines = [line.strip()]
        paren_balance = line.count("(") - line.count(")")
        while paren_balance > 0 and index + 1 < len(lines):
            index += 1
            signature_lines.append(lines[index].strip())
            paren_balance += lines[index].count("(") - lines[index].count(")")
        signature = normalize_space(" ".join(signature_lines))
        return_type = clean_generic(signature_match.group(1))
        method_name = signature_match.group(2)
        params_text_match = re.search(r"\((.*)\)", signature)
        params_text = params_text_match.group(1) if params_text_match else ""

        source_text = "\n".join(lines[index:])
        body, _ = extract_method_body(source_text, source_text.find("{"))
        http_annotation = next(
            (item for item in annotations if re.match(r"@(Get|Post|Put|Patch|Delete)Mapping", item)),
            None,
        )
        if not http_annotation:
            annotations = []
            index += body.count("\n") + 1
            continue
        http_method = re.match(r"@(\w+)Mapping", http_annotation).group(1).replace("Mapping", "").upper()
        sub_paths = parse_mapping_paths(http_annotation)
        full_paths = [f"{base_path}{sub_path}" for sub_path in sub_paths]
        permission_annotation = next((item for item in annotations if item.startswith("@PreAuthorize")), None)
        permission = parse_permission(permission_annotation)
        messages = re.findall(r'\.message\("([^"]*)"\)', body)
        status_match = re.search(r"\.status\((\d+)\)", body)
        status_code = int(status_match.group(1)) if status_match else 200
        request_body_type = None
        path_params: list[ParamInfo] = []
        query_params: list[ParamInfo] = []
        file_params: list[ParamInfo] = []
        for param_text in split_parameters(params_text):
            parsed, body_type = parse_single_param(param_text, enums)
            if body_type:
                request_body_type = body_type
                continue
            if not parsed:
                continue
            if parsed.source == "path":
                path_params.append(parsed)
            elif parsed.source == "query":
                query_params.append(parsed)
            elif parsed.source == "file":
                file_params.append(parsed)

        service_call_match = None
        for field_name in field_services:
            search = re.search(rf"{re.escape(field_name)}\.(\w+)\(", body)
            if search:
                service_call_match = (field_name, search.group(1))
                break
        response_type = None
        response_wrapper = "api"
        notes: list[str] = []
        if return_type.startswith("ResponseEntity<byte[]>"):
            response_wrapper = "file"
            response_type = "byte[]"
            notes.append("API trả về tệp Excel nhị phân, client cần xử lý theo header Content-Disposition.")
        elif return_type == "void":
            response_wrapper = "empty"
            response_type = None
            notes.append("API không trả về body; chỉ dùng cho luồng callback nội bộ.")
        else:
            data_present = ".data(" in body
            if service_call_match:
                service_type = field_services[service_call_match[0]]
                response_type = service_returns.get(service_type, {}).get(service_call_match[1])
            if not data_present:
                response_type = None
                notes.append("Phản hồi chỉ chứa status và message, không có trường data.")
        if request_body_type == "Map<String, Object>":
            notes.append("Payload dạng động, được dùng để tích hợp PayOS hoặc webhook; cần tuân thủ đúng contract của bên thứ ba.")
        if full_paths and any("/upload" in item for item in full_paths):
            notes.append("Yêu cầu gửi theo multipart/form-data, trường file bắt buộc tên là file.")
        if query_params and any(param.name == "page" and param.default == "0" for param in query_params):
            notes.append("API này đang dùng page index bắt đầu từ 0.")
        elif query_params and any(param.name == "page" and param.default == "1" for param in query_params):
            notes.append("API này đang dùng page index bắt đầu từ 1.")

        content_type = None
        if file_params:
            content_type = "multipart/form-data"
        elif request_body_type:
            content_type = "application/json"

        method_counter += 1
        code = f"{MODULE_CODES[module_title]}-{method_counter:02d}"
        message = messages[-1] if messages else ""
        description = message or "API xử lý nghiệp vụ theo controller hiện tại."
        title = parse_method_title(message, method_name, full_paths)
        endpoints.append(
            EndpointInfo(
                controller_name=controller_name,
                module_title=module_title,
                code=code,
                method_name=method_name,
                http_method=http_method,
                paths=full_paths,
                permission=permission,
                title=title,
                description=description,
                status_code=status_code,
                response_wrapper=response_wrapper,
                response_type=response_type,
                request_body_type=request_body_type,
                path_params=path_params,
                query_params=query_params,
                file_params=file_params,
                content_type=content_type,
                notes=notes,
            )
        )
        annotations = []
        index += body.count("\n") + 1
    return endpoints


def build_success_example(
    endpoint: EndpointInfo,
    request_dtos: dict[str, list[FieldInfo]],
    response_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
) -> str:
    if endpoint.response_wrapper == "file":
        return """HTTP/1.1 200 OK
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="sample.xlsx"

<binary excel content>"""
    if endpoint.response_wrapper == "empty":
        return """HTTP/1.1 200 OK

<empty body>"""
    payload: dict[str, Any] = {
        "status": endpoint.status_code,
        "message": endpoint.description,
    }
    if endpoint.response_type:
        payload["data"] = sample_for_type(endpoint.response_type, request_dtos, response_dtos, enums)
    return json.dumps(payload, ensure_ascii=False, indent=2)


def build_request_example(
    endpoint: EndpointInfo,
    request_dtos: dict[str, list[FieldInfo]],
    response_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
) -> str | None:
    if endpoint.content_type == "multipart/form-data":
        parts = []
        for file_param in endpoint.file_params:
            parts.append(f'{file_param.name}: <binary file>')
        return "\n".join(parts)
    if not endpoint.request_body_type:
        return None
    if endpoint.request_body_type == "Map<String, Object>":
        example = {"donationMemoCode": "MEMO2026001"}
        if any("/payos-ipn" in item for item in endpoint.paths):
            example = {
                "code": "00",
                "desc": "success",
                "data": {"orderCode": 10001, "amount": 500000},
                "signature": "sample-signature",
            }
        return json.dumps(example, ensure_ascii=False, indent=2)
    payload = sample_for_type(endpoint.request_body_type, request_dtos, response_dtos, enums)
    return json.dumps(payload, ensure_ascii=False, indent=2)


def field_rows_html(
    fields: list[FieldInfo],
    request_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
    include_sample: bool = True,
) -> str:
    rows: list[str] = []
    for field_info in fields:
        required = "Có" if any(item.startswith("@Not") for item in field_info.annotations) else "Không"
        validation = describe_validation(field_info.annotations, field_info.type_name, enums)
        sample = sample_for_type(field_info.type_name, request_dtos, {}, enums, field_info.name)
        sample_text = json.dumps(sample, ensure_ascii=False) if include_sample else ""
        rows.append(
            "<tr>"
            f"<td>{html.escape(field_info.name)}</td>"
            f"<td>{html.escape(field_info.type_name)}</td>"
            f"<td>{required}</td>"
            f"<td>{html.escape(validation)}</td>"
            f"<td>{html.escape(sample_text)}</td>"
            "</tr>"
        )
    return "".join(rows)


def params_table_html(title: str, params: list[ParamInfo]) -> str:
    if not params:
        return ""
    rows = []
    for param in params:
        required = "Có" if param.required is True else "Không"
        rows.append(
            "<tr>"
            f"<td>{html.escape(param.name)}</td>"
            f"<td>{html.escape(param.type_name)}</td>"
            f"<td>{required}</td>"
            f"<td>{html.escape(param.default or '')}</td>"
            f"<td>{html.escape(param.note)}</td>"
            "</tr>"
        )
    return (
        f"<h5>{html.escape(title)}</h5>"
        "<table>"
        "<tr><th>Tên tham số</th><th>Kiểu dữ liệu</th><th>Bắt buộc</th><th>Mặc định</th><th>Ghi chú</th></tr>"
        + "".join(rows)
        + "</table>"
    )


def enum_table_html(enums: dict[str, list[dict[str, str | None]]]) -> str:
    rows: list[str] = []
    for enum_name in sorted(
        [
            "EActivityStatus",
            "EEventStatus",
            "EDonationStatus",
            "EDonationTarget",
            "EDonationType",
            "EDonationVia",
            "EDonorType",
            "EDonorWallPeriod",
            "EDashboardPeriod",
            "EUserStatus",
            "ERole",
            "EPaymentMethod",
            "EAuditAction",
            "EEntityType",
        ]
    ):
        for item in enums.get(enum_name, []):
            rows.append(
                "<tr>"
                f"<td>{html.escape(enum_name)}</td>"
                f"<td>{html.escape(item['name'] or '')}</td>"
                f"<td>{html.escape(item['label'] or '')}</td>"
                "</tr>"
            )
    return (
        "<table>"
        "<tr><th>Enum</th><th>Giá trị</th><th>Ý nghĩa</th></tr>"
        + "".join(rows)
        + "</table>"
    )


def error_response_html() -> str:
    example = {
        "timestamp": "2026-04-11T18:00:00+07:00",
        "status": 400,
        "path": "/api/events",
        "error": "Invalid Payload",
        "message": "Tên sự kiện không được để trống",
    }
    statuses = [
        ("400", "Dữ liệu đầu vào không hợp lệ, thiếu tham số hoặc vi phạm validate."),
        ("401", "Sai thông tin đăng nhập tại trang /login, không phải API login riêng."),
        ("403", "Người dùng không đủ quyền role để gọi API."),
        ("404", "Không tìm thấy dữ liệu theo id hoặc theo điều kiện truy vấn."),
        ("409", "Dữ liệu trùng hoặc xung đột nghiệp vụ, ví dụ email/điện thoại nhà hảo tâm đã tồn tại."),
        ("500", "Lỗi nội bộ hệ thống hoặc lỗi tích hợp ngoài."),
    ]
    rows = "".join(
        f"<tr><td>{code}</td><td>{html.escape(description)}</td></tr>"
        for code, description in statuses
    )
    return (
        "<h3>4.4. Mẫu phản hồi lỗi</h3>"
        "<p>Tất cả API trong package <code>com.chiaseyeuthuong.api</code> dùng chung cấu trúc lỗi gồm "
        "<code>timestamp</code>, <code>status</code>, <code>path</code>, <code>error</code>, <code>message</code>.</p>"
        "<pre>"
        + html.escape(json.dumps(example, ensure_ascii=False, indent=2))
        + "</pre>"
        "<table><tr><th>Mã HTTP</th><th>Ý nghĩa áp dụng trong hệ thống</th></tr>"
        + rows
        + "</table>"
    )


def common_response_html() -> str:
    example = {
        "status": 200,
        "message": "Lấy danh sách quyên góp thành công",
        "data": {
            "page": 1,
            "pageSize": 10,
            "totalPages": 1,
            "totalItems": 1,
            "data": [
                {
                    "id": 1,
                    "amount": 500000,
                    "status": "CONFIRMED",
                    "donorName": "Nguyen Van A",
                }
            ],
        },
    }
    return (
        "<h3>4.3. Cấu trúc phản hồi thành công</h3>"
        "<p>Phần lớn API trả về đối tượng chuẩn <code>ApiResponse</code> gồm các trường <code>status</code>, "
        "<code>message</code> và <code>data</code>. Một số API đặc biệt trả về tệp Excel hoặc body rỗng.</p>"
        "<pre>"
        + html.escape(json.dumps(example, ensure_ascii=False, indent=2))
        + "</pre>"
        "<p>Đối với các API phân trang, phần <code>data</code> bên trong tiếp tục dùng cấu trúc <code>PageResponse</code> "
        "gồm <code>page</code>, <code>pageSize</code>, <code>totalPages</code>, <code>totalItems</code> và danh sách <code>data</code>.</p>"
    )


def build_cover_html(base_url: str, endpoint_count: int) -> str:
    today = date.today().strftime("%d/%m/%Y")
    return f"""
    <div class="page cover-page">
      <div class="cover-frame">
        <div class="cover-top">HỆ THỐNG DONATION MANAGEMENT</div>
        <div class="cover-subtop">TÀI LIỆU ĐẶC TẢ API</div>
        <div class="cover-divider"></div>
        <div class="cover-title">ĐẶC TẢ GIAO TIẾP LẬP TRÌNH ỨNG DỤNG</div>
        <div class="cover-subtitle">Dựa trên hiện trạng mã nguồn backend Spring Boot</div>
        <table class="meta-table">
          <tr><td>Tên hệ thống</td><td>DonationManagement</td></tr>
          <tr><td>Backend</td><td>Spring Boot, Spring Security, JPA, MySQL</td></tr>
          <tr><td>Base URL mặc định</td><td>{html.escape(base_url)}</td></tr>
          <tr><td>Số lượng endpoint đặc tả</td><td>{endpoint_count}</td></tr>
          <tr><td>Ngày biên soạn</td><td>{today}</td></tr>
          <tr><td>Ghi chú</td><td>Tài liệu được sinh từ controller, DTO, service và cấu hình ứng dụng hiện tại</td></tr>
        </table>
        <div class="cover-footer">Đà Nẵng, {today}</div>
      </div>
    </div>
    """


def build_toc_html(grouped: dict[str, list[EndpointInfo]]) -> str:
    items = [
        "1. Mục đích tài liệu",
        "2. Phạm vi áp dụng",
        "3. Nguồn thông tin dùng để đặc tả",
        "4. Quy ước tích hợp chung",
        "5. Danh mục enum nghiệp vụ",
        "6. Đặc tả chi tiết API",
    ]
    for index, module in enumerate(MODULE_ORDER, start=1):
        if grouped.get(module):
            items.append(f"6.{index}. Nhóm API {module}")
    return (
        "<div class='page'>"
        "<h1 class='center'>MỤC LỤC</h1>"
        "<table class='toc-table'>"
        + "".join(f"<tr><td>{html.escape(item)}</td></tr>" for item in items)
        + "</table>"
        "</div>"
    )


def build_intro_html(base_url: str) -> str:
    return f"""
    <div class="page">
      <h1>1. MỤC ĐÍCH TÀI LIỆU</h1>
      <p>Tài liệu này mô tả chi tiết các API đang được cung cấp bởi hệ thống DonationManagement nhằm phục vụ
      cho việc tích hợp frontend, kiểm thử, bàn giao kỹ thuật và đối soát nghiệp vụ. Văn phong và cách tổ chức
      được trình bày theo hướng trang trọng, có đánh số đề mục, bảng thông tin và ví dụ minh họa giống tinh thần
      của tài liệu tham khảo đã cung cấp.</p>

      <h1>2. PHẠM VI ÁP DỤNG</h1>
      <p>Đặc tả bao phủ toàn bộ controller REST tại package <code>com.chiaseyeuthuong.api</code>, bao gồm API công khai,
      API back-office có phân quyền role, API tích hợp PayOS, webhook nội bộ và API import/export Excel.</p>

      <h1>3. NGUỒN THÔNG TIN DÙNG ĐỂ ĐẶC TẢ</h1>
      <p>Nội dung tài liệu được tổng hợp trực tiếp từ các file controller, request DTO, response DTO, service interface,
      enum nghiệp vụ, cấu hình bảo mật và bộ xử lý ngoại lệ toàn cục trong mã nguồn hiện tại. Vì vậy đặc tả bám sát
      triển khai thực tế thay vì mô tả ở mức giả định.</p>

      <h1>4. QUY ƯỚC TÍCH HỢP CHUNG</h1>
      <h3>4.1. Base URL</h3>
      <p>Base URL mặc định của ứng dụng theo <code>application.yml</code> là <code>{html.escape(base_url)}</code>.
      Khi triển khai môi trường khác, giá trị này có thể thay đổi qua biến môi trường <code>APP_BASE_URL</code>.</p>

      <h3>4.2. Xác thực và phân quyền</h3>
      <p>Hệ thống đang dùng Spring Security theo cơ chế <b>form login + session</b>, không có REST API login riêng theo kiểu JWT.
      Sau khi đăng nhập qua <code>/login</code>, client gọi các API back-office bằng phiên đăng nhập hiện hữu. Phân quyền áp dụng
      theo các vai trò <code>ADMIN</code>, <code>ACCOUNTING</code>, <code>STAFF</code>, <code>DONOR</code>.</p>

      {common_response_html()}
      {error_response_html()}
    </div>
    """


def build_request_body_sections(
    dto_name: str,
    request_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
    rendered: set[str] | None = None,
) -> str:
    rendered = rendered or set()
    if dto_name in rendered or dto_name not in request_dtos:
        return ""
    rendered.add(dto_name)
    fields = request_dtos[dto_name]
    section = (
        f"<h5>Cấu trúc payload: {html.escape(dto_name)}</h5>"
        "<table>"
        "<tr><th>Trường</th><th>Kiểu dữ liệu</th><th>Bắt buộc</th><th>Ràng buộc / diễn giải</th><th>Ví dụ</th></tr>"
        + field_rows_html(fields, request_dtos, enums)
        + "</table>"
    )
    for field_info in fields:
        nested_type = field_info.type_name
        if nested_type.startswith("List<") and nested_type.endswith(">"):
            nested_type = nested_type[5:-1].strip()
        if nested_type in request_dtos and nested_type != dto_name:
            section += build_request_body_sections(nested_type, request_dtos, enums, rendered)
    return section


def build_endpoint_html(
    endpoint: EndpointInfo,
    request_dtos: dict[str, list[FieldInfo]],
    response_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
) -> str:
    request_example = build_request_example(endpoint, request_dtos, response_dtos, enums)
    success_example = build_success_example(endpoint, request_dtos, response_dtos, enums)
    notes_html = ""
    if endpoint.notes:
        notes_html = "<ul>" + "".join(f"<li>{html.escape(note)}</li>" for note in endpoint.notes) + "</ul>"
    meta_rows = [
        ("Mã API", endpoint.code),
        ("Tên API", endpoint.title),
        ("Phương thức", endpoint.http_method),
        ("Đường dẫn", "<br/>".join(html.escape(item) for item in endpoint.paths)),
        ("Phân quyền", endpoint.permission),
        ("Content-Type", endpoint.content_type or "Không yêu cầu body"),
        ("HTTP status thành công", str(endpoint.status_code)),
        ("Kiểu dữ liệu trả về", endpoint.response_type or ("Không có body dữ liệu" if endpoint.response_wrapper != "file" else "Tệp Excel nhị phân")),
        ("Mô tả", endpoint.description),
    ]
    meta_table = (
        "<table class='meta-grid'>"
        + "".join(
            f"<tr><th>{html.escape(label)}</th><td>{value}</td></tr>"
            for label, value in meta_rows
        )
        + "</table>"
    )
    parts = [
        f"<div class='endpoint'><h4>{html.escape(endpoint.code)}. {html.escape(endpoint.title)}</h4>",
        meta_table,
        params_table_html("Tham số đường dẫn", endpoint.path_params),
        params_table_html("Tham số truy vấn / form", endpoint.query_params + endpoint.file_params),
    ]
    if endpoint.request_body_type:
        parts.append(build_request_body_sections(endpoint.request_body_type, request_dtos, enums))
    if request_example:
        parts.append("<h5>Ví dụ request</h5><pre>" + html.escape(request_example) + "</pre>")
    parts.append("<h5>Ví dụ phản hồi thành công</h5><pre>" + html.escape(success_example) + "</pre>")
    if notes_html:
        parts.append("<h5>Lưu ý triển khai</h5>" + notes_html)
    parts.append("</div>")
    return "".join(part for part in parts if part)


def build_api_detail_html(
    grouped: dict[str, list[EndpointInfo]],
    request_dtos: dict[str, list[FieldInfo]],
    response_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
) -> str:
    sections = ["<div class='page'><h1>5. DANH MỤC ENUM NGHIỆP VỤ</h1>", enum_table_html(enums), "<h1>6. ĐẶC TẢ CHI TIẾT API</h1>"]
    sub_index = 1
    for module in MODULE_ORDER:
        endpoints = grouped.get(module, [])
        if not endpoints:
            continue
        sections.append(f"<h2>6.{sub_index}. NHÓM API {html.escape(module).upper()}</h2>")
        for endpoint in endpoints:
            sections.append(build_endpoint_html(endpoint, request_dtos, response_dtos, enums))
        sub_index += 1
    sections.append("</div>")
    return "".join(sections)


def build_html_document(
    base_url: str,
    endpoints: list[EndpointInfo],
    request_dtos: dict[str, list[FieldInfo]],
    response_dtos: dict[str, list[FieldInfo]],
    enums: dict[str, list[dict[str, str | None]]],
) -> str:
    grouped: dict[str, list[EndpointInfo]] = defaultdict(list)
    for endpoint in endpoints:
        grouped[endpoint.module_title].append(endpoint)
    for items in grouped.values():
        items.sort(key=lambda item: item.code)
    return f"""<!DOCTYPE html>
<html lang="vi">
<head>
  <meta charset="utf-8" />
  <title>Đặc tả API DonationManagement</title>
  <style>
    body {{
      font-family: "Times New Roman", serif;
      font-size: 13pt;
      line-height: 1.45;
      color: #111111;
      margin: 0;
      padding: 0;
    }}
    .page {{
      padding: 28px 34px;
      page-break-after: always;
    }}
    .page:last-child {{
      page-break-after: auto;
    }}
    .center {{
      text-align: center;
    }}
    .cover-page {{
      padding: 48px 56px;
    }}
    .cover-frame {{
      border: 3px solid #111111;
      min-height: 980px;
      padding: 36px 28px;
      position: relative;
    }}
    .cover-top {{
      text-align: center;
      font-size: 18pt;
      font-weight: bold;
      margin-top: 10px;
    }}
    .cover-subtop {{
      text-align: center;
      font-size: 20pt;
      font-weight: bold;
      margin-top: 18px;
    }}
    .cover-divider {{
      width: 45%;
      border-bottom: 1px solid #555555;
      margin: 14px auto 42px auto;
    }}
    .cover-title {{
      text-align: center;
      font-size: 24pt;
      font-weight: bold;
      margin-top: 60px;
    }}
    .cover-subtitle {{
      text-align: center;
      font-size: 16pt;
      margin-top: 18px;
      margin-bottom: 64px;
    }}
    .cover-footer {{
      position: absolute;
      left: 0;
      right: 0;
      bottom: 40px;
      text-align: center;
      font-weight: bold;
      font-size: 16pt;
    }}
    h1, h2, h3, h4, h5 {{
      font-weight: bold;
      margin: 16px 0 8px 0;
    }}
    h1 {{
      font-size: 18pt;
      text-transform: uppercase;
    }}
    h2 {{
      font-size: 16pt;
      text-transform: uppercase;
      border-bottom: 1px solid #333333;
      padding-bottom: 4px;
    }}
    h3 {{
      font-size: 14pt;
    }}
    h4 {{
      font-size: 13.5pt;
      margin-top: 22px;
    }}
    h5 {{
      font-size: 13pt;
      margin-top: 14px;
    }}
    p {{
      text-align: justify;
      margin: 8px 0;
    }}
    table {{
      border-collapse: collapse;
      width: 100%;
      margin: 10px 0 14px 0;
    }}
    th, td {{
      border: 1px solid #222222;
      padding: 6px 8px;
      vertical-align: top;
    }}
    th {{
      background: #f0f0f0;
      text-align: left;
      font-weight: bold;
    }}
    .meta-table {{
      width: 88%;
      margin: 30px auto 0 auto;
    }}
    .toc-table td {{
      border: none;
      padding: 6px 4px;
    }}
    .meta-grid th {{
      width: 23%;
    }}
    .endpoint {{
      margin-bottom: 24px;
    }}
    pre {{
      white-space: pre-wrap;
      word-break: break-word;
      border: 1px solid #444444;
      padding: 10px;
      background: #fafafa;
      font-size: 10.5pt;
      line-height: 1.35;
    }}
    code {{
      font-family: "Courier New", monospace;
      font-size: 11pt;
    }}
    ul {{
      margin: 6px 0 10px 18px;
    }}
    li {{
      margin: 4px 0;
    }}
  </style>
</head>
<body>
  {build_cover_html(base_url, len(endpoints))}
  {build_toc_html(grouped)}
  {build_intro_html(base_url)}
  {build_api_detail_html(grouped, request_dtos, response_dtos, enums)}
</body>
</html>
"""


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    enums = parse_enums()
    request_dtos = parse_dto_fields(DTO_REQUEST_DIR)
    response_dtos = parse_dto_fields(DTO_RESPONSE_DIR)
    service_returns = parse_service_returns()
    endpoints: list[EndpointInfo] = []
    for path in sorted(API_DIR.glob("*.java")):
        endpoints.extend(parse_controller(path, service_returns, enums))
    endpoints.sort(key=lambda item: (MODULE_ORDER.index(item.module_title), item.code) if item.module_title in MODULE_ORDER else (999, item.code))
    html_doc = build_html_document(parse_base_url(), endpoints, request_dtos, response_dtos, enums)
    OUTPUT_HTML.write_text(html_doc, encoding="utf-8")
    subprocess.run(
        [
            "textutil",
            "-convert",
            "docx",
            str(OUTPUT_HTML),
            "-output",
            str(OUTPUT_DOCX),
        ],
        check=True,
    )
    print(f"Generated {OUTPUT_HTML}")
    print(f"Generated {OUTPUT_DOCX}")


if __name__ == "__main__":
    main()
