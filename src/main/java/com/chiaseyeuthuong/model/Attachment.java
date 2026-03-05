package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.EAttachmentType;
import com.chiaseyeuthuong.common.EEntityType;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_id")
    private Long objectId;

    @Column(name = "entity_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EEntityType entityType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    @Enumerated(EnumType.STRING)
    private EAttachmentType type;
}
